package tech.wetech.admin3.sys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.Invoice;
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.model.ReimbursementExpenseItem;
import tech.wetech.admin3.sys.repository.InvoiceRepository;
import tech.wetech.admin3.sys.repository.ReimbursementExpenseItemRepository;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OcrServiceTest {

  private final OcrService ocrService = new OcrService(null, null, null, null, null);

  @Test
  void extractTotalAmountFromTextReadsAmountAfterLowercaseMarker() {
    String ocrText = """
      价税合计（大写）
      )壹拾贰圆陆角整
      （小写）￥12.60
      """;

    String total = ocrService.extractTotalAmountFromText(ocrText);

    assertThat(total).isEqualTo("12.60");
  }

  @Test
  void reconcileTotalAmountFromSummaryRestoresDroppedLeadingDigit() {
    String total = ocrService.reconcileTotalAmountFromSummary("2.60", List.of("12.23", "0.37"));

    assertThat(total).isEqualTo("12.60");
  }

  @Test
  void reconcileTotalAmountFromSummaryKeepsUnrelatedTotalAmount() {
    String total = ocrService.reconcileTotalAmountFromSummary("15.24", List.of("12.23", "0.37"));

    assertThat(total).isEqualTo("15.24");
  }

  @Test
  void extractInvoiceFieldsFromTextRecognizesRailwayElectronicTicketFields() throws Exception {
    String railwayTicketText = """
      电子发票（铁路电子客票）
      发票号码：24110101234567890123
      开票日期：2025年01月02日
      购买方名称：示例科技有限公司
      行程信息：北京南站至上海虹桥站
      票价：￥128.50
      """;

    Map<String, String> fields = extractStructuredFields(railwayTicketText, "railway-ticket.pdf");

    assertThat(fields)
      .containsEntry("invoice_no", "24110101234567890123")
      .containsEntry("date", "2025年01月02日")
      .containsEntry("total_amount", "128.50")
      .containsEntry("invoice_type", "电子发票（铁路电子客票）");
  }

  @Test
  void extractInvoiceFieldsFromTextUsesLowercaseTotalForNonTaxTollInvoice() throws Exception {
    String tollInvoiceText = """
      \u7535\u5b50\u53d1\u7968\uff08\u666e\u901a\u53d1\u7968\uff09
      \u53d1\u7968\u53f7\u7801\uff1a26447000001447993351
      *\u751f\u4ea7\u751f\u6d3b\u670d\u52a1*\u4ee3\u6536\u901a\u884c\u8d39 1 40.0 40.00 \u4e0d\u5f81\u7a0e
      \u5408\u8ba1 \u00a540.00 \u00a50.00
      \u4ef7\u7a0e\u5408\u8ba1\uff08\u5927\u5199\uff09\u8086\u62fe\u5706\u6574 \uff08\u5c0f\u5199\uff09\u00a540.00
      """;

    assertThat(tollInvoiceText).contains("\u4e0d\u5f81\u7a0e");
    Map<String, String> fields = extractStructuredFields(tollInvoiceText, "高速票.pdf");

    assertThat(fields)
      .containsEntry("total_amount", "40.00")
      .containsEntry("amount_before_tax", "40.00")
      .containsEntry("tax_amount", "0.00");
  }

  @Test
  void extractInvoiceFieldsFromPdfBoxTextReadsTollInvoiceLayout() throws Exception {
    String tollInvoiceText = """
      \u7535\u5b50\u53d1\u7968\uff08\u666e\u901a\u53d1\u7968\uff09
      \u53d1\u7968\u53f7\u7801\uff1a
      \u5f00\u7968\u65e5\u671f\uff1a
      \u8d2d
      \u4e70
      \u65b9
      \u4fe1
      \u606f
      \u7edf\u4e00\u793e\u4f1a\u4fe1\u7528\u4ee3\u7801/\u7eb3\u7a0e\u4eba\u8bc6\u522b\u53f7\uff1a
      \u9500
      \u552e
      \u65b9
      \u4fe1
      \u606f
      \u7edf\u4e00\u793e\u4f1a\u4fe1\u7528\u4ee3\u7801/\u7eb3\u7a0e\u4eba\u8bc6\u522b\u53f7\uff1a
      91441900762909683X 914400007408467653
      2026\u5e7407\u670822\u65e5
      40.00?
      \u5e7f\u4e1c\u7ef4\u4fe1\u667a\u8054\u79d1\u6280\u6709\u9650\u516c\u53f8
      0.00?
      \u5e7f\u4e1c\u8054\u5408\u7535\u5b50\u670d\u52a1\u80a1\u4efd\u6709\u9650\u516c\u53f8
      26447000001447993351
      40.00?
      ****\u751f\u4ea7\u751f\u6d3b\u670d\u52a1*\u4ee3\u6536\u901a\u884c
      \u8d39
      40.0*** 1 40.00 \u4e0d\u5f81\u7a0e
      """;

    Map<String, String> fields = extractStructuredFields(tollInvoiceText, "\u9ad8\u901f\u7968.pdf");

    assertThat(fields)
      .containsEntry("invoice_no", "26447000001447993351")
      .containsEntry("date", "2026\u5e7407\u670822\u65e5")
      .containsEntry("buyer_name", "\u5e7f\u4e1c\u7ef4\u4fe1\u667a\u8054\u79d1\u6280\u6709\u9650\u516c\u53f8")
      .containsEntry("buyer_tax_id", "91441900762909683X")
      .containsEntry("seller_name", "\u5e7f\u4e1c\u8054\u5408\u7535\u5b50\u670d\u52a1\u80a1\u4efd\u6709\u9650\u516c\u53f8")
      .containsEntry("seller_tax_id", "914400007408467653")
      .containsEntry("total_amount", "40.00")
      .containsEntry("amount_before_tax", "40.00")
      .containsEntry("tax_amount", "0.00");
  }

  @Test
  void extractInvoiceFieldsFromPdfBoxTextReadsAirTicketSummaryAmounts() throws Exception {
    String airTicketText = """
      \u7535\u5b50\u53d1\u7968\uff08\u666e\u901a\u53d1\u7968\uff09 \u53d1\u7968\u53f7\u7801\uff1a
      \u5f00\u7968\u65e5\u671f\uff1a
      \u9500
      \u552e
      \u65b9
      \u4fe1
      \u606f
      \u7edf\u4e00\u793e\u4f1a\u4fe1\u7528\u4ee3\u7801/\u7eb3\u7a0e\u4eba\u8bc6\u522b\u53f7\uff1a
      \u540d\u79f0\uff1a\u8d2d
      \u4e70
      \u65b9
      \u4fe1\u606f
      \u7edf\u4e00\u793e\u4f1a\u4fe1\u7528\u4ee3\u7801/\u7eb3\u7a0e\u4eba\u8bc6\u522b\u53f7\uff1a
      \u540d\u79f0\uff1a
      26127000000363374667
      2026\u5e7407\u670822\u65e5
      \u5e7f\u4e1c\u7ef4\u4fe1\u667a\u8054\u79d1\u6280\u6709\u9650\u516c\u53f8
      91441900762909683X
      \u5929\u6d25\u822a\u8def\u65e0\u754c\u56fd\u9645\u65c5\u884c\u793e\u6709\u9650\u516c\u53f8
      91120222MAETKCJ87K
      \u9879\u76ee\u540d\u79f0 \u89c4\u683c\u578b\u53f7 \u5355\u4f4d \u6570\u91cf \u5355\u4ef7 \u91d1\u989d \u7a0e\u7387/\u5f81\u6536\u7387 \u7a0e\u989d
      *\u751f\u4ea7\u751f\u6d3b\u670d\u52a1*\u4ee3\u8ba2\u673a\u7968 \u6b21 1 1781.132075471698 1781.13 6% 106.87
      \u5957\u9910\u4ea7\u54c1
      *\u751f\u4ea7\u751f\u6d3b\u670d\u52a1*\u7ecf\u7eaa\u4ee3\u7406 \u6b21 1 90.5660377358491 90.57 6% 5.43
      \u670d\u52a1\u8d39
      \u4ef7\u7a0e\u5408\u8ba1\uff08\u5927\u5199\uff09
      \u5408\u8ba1
      \uff08\u5c0f\u5199\uff09
      \u5907\u6ce8
      \u5f00\u7968\u4eba\uff1a
      \u58f9\u4edf\u7396\u4f70\u634c\u62fe\u8086\u5706\u6574 ?1984.00
      \u5468\u6676
      1871.70? 112.30?
      """;

    Map<String, String> fields = extractStructuredFields(airTicketText, "2026-07-22-air-ticket-1984.00.pdf");

    assertThat(fields)
      .containsEntry("invoice_no", "26127000000363374667")
      .containsEntry("total_amount", "1984.00")
      .containsEntry("amount_before_tax", "1871.70")
      .containsEntry("tax_amount", "112.30")
      .containsEntry("issuer", "\u5468\u6676");
  }

  @Test
  void extractInvoiceFieldsFromPdfBoxTextReadsVatSpecialInvoiceSummaryAmounts() throws Exception {
    String hotelInvoiceText = """
      \u7535\u5b50\u53d1\u7968\uff08\u589e\u503c\u7a0e\u4e13\u7528\u53d1\u7968\uff09 \u53d1\u7968\u53f7\u7801\uff1a
      \u5f00\u7968\u65e5\u671f\uff1a
      \u8d2d
      \u4e70
      \u65b9
      \u4fe1
      \u606f
      \u7edf\u4e00\u793e\u4f1a\u4fe1\u7528\u4ee3\u7801/\u7eb3\u7a0e\u4eba\u8bc6\u522b\u53f7\uff1a
      \u9500
      \u552e
      \u65b9
      \u4fe1
      \u606f
      \u7edf\u4e00\u793e\u4f1a\u4fe1\u7528\u4ee3\u7801/\u7eb3\u7a0e\u4eba\u8bc6\u522b\u53f7\uff1a
      \u540d\u79f0\uff1a \u540d\u79f0\uff1a
      \u9879\u76ee\u540d\u79f0 \u89c4\u683c\u578b\u53f7 \u5355\u4f4d \u6570\u91cf \u5355\u4ef7 \u91d1\u989d \u7a0e\u7387/\u5f81\u6536\u7387 \u7a0e\u989d
      \u5408\u8ba1
      \u4ef7\u7a0e\u5408\u8ba1\uff08\u5927\u5199\uff09 \uff08\u5c0f\u5199\uff09
      \u5907\u6ce8
      \u5f00\u7968\u4eba\uff1a
      26112000003078497746
      2026\u5e7407\u670824\u65e5
      \u5e7f\u4e1c\u7ef4\u4fe1\u667a\u8054\u79d1\u6280\u6709\u9650\u516c\u53f8
      91441900762909683X
      \u5317\u4eac\u738b\u5e9c\u4e95\u6c38\u5149\u5bbe\u9986\u7ba1\u7406\u6709\u9650\u516c\u53f8\u738b\u5e9c\u4e95\u5206\u516c\u53f8
      91110101633604904T
      \u00a5758.49 \u00a545.51
      \u634c\u4f70\u96f6\u8086\u5706\u6574 \u00a5804.00
      \u674e\u9756
      \u674e\u9756
      *\u751f\u4ea7\u751f\u6d3b\u670d\u52a1*\u4f4f\u5bbf\u8d39 6%\u5929 758.49 45.51379.24528301886792
      """;

    Map<String, String> fields = extractStructuredFields(hotelInvoiceText, "hotel.pdf");

    assertThat(fields)
      .containsEntry("invoice_no", "26112000003078497746")
      .containsEntry("buyer_name", "\u5e7f\u4e1c\u7ef4\u4fe1\u667a\u8054\u79d1\u6280\u6709\u9650\u516c\u53f8")
      .containsEntry("seller_name", "\u5317\u4eac\u738b\u5e9c\u4e95\u6c38\u5149\u5bbe\u9986\u7ba1\u7406\u6709\u9650\u516c\u53f8\u738b\u5e9c\u4e95\u5206\u516c\u53f8")
      .containsEntry("total_amount", "804.00")
      .containsEntry("amount_before_tax", "758.49")
      .containsEntry("tax_amount", "45.51")
      .containsEntry("issuer", "\u674e\u9756");
  }

  @Test
  void extractInvoiceFieldsFromPdfBoxTextReadsRestaurantInvoiceIssuer() throws Exception {
    String restaurantInvoiceText = """
      \u7535\u5b50\u53d1\u7968\uff08\u666e\u901a\u53d1\u7968\uff09 \u53d1\u7968\u53f7\u7801\uff1a
      \u4ef7\u7a0e\u5408\u8ba1\uff08\u5927\u5199\uff09 \uff08\u5c0f\u5199\uff09
      \u5f00\u7968\u4eba\uff1a
      26112000003059680516
      2026\u5e7407\u670823\u65e5
      \u5e7f\u4e1c\u7ef4\u4fe1\u667a\u8054\u79d1\u6280\u6709\u9650\u516c\u53f8
      91441900762909683X
      \u5317\u4eac\u4eac\u5bfb\u9910\u996e\u6709\u9650\u516c\u53f8
      91110101MA02A3E27L
      \u00a5172.77 \u00a51.73
      \u58f9\u4f70\u67d2\u62fe\u8086\u5706\u4f0d\u89d2\u6574 \u00a5174.50
      \u674e\u7d20\u534e
      \u674e\u7d20\u534e
      *\u751f\u4ea7\u751f\u6d3b\u670d\u52a1*\u9910\u996e\u670d\u52a1 1%172.77 1.73172.771
      """;

    Map<String, String> fields = extractStructuredFields(restaurantInvoiceText, "restaurant.pdf");

    assertThat(fields)
      .containsEntry("total_amount", "174.50")
      .containsEntry("issuer", "\u674e\u7d20\u534e");
  }

  @Test
  void extractInvoiceFieldsFromPdfBoxTextKeepsAirTicketSummaryOrder() throws Exception {
    String invoiceText = """
      \u7535\u5b50\u53d1\u7968\uff08\u666e\u901a\u53d1\u7968\uff09 \u53d1\u7968\u53f7\u7801\uff1a
      \u4ef7\u7a0e\u5408\u8ba1\uff08\u5927\u5199\uff09
      \u5408\u8ba1
      \uff08\u5c0f\u5199\uff09
      \u5f00\u7968\u4eba\uff1a
      26317000002664059212
      2026\u5e7407\u670825\u65e5
      \u5e7f\u4e1c\u7ef4\u4fe1\u667a\u8054\u79d1\u6280\u6709\u9650\u516c\u53f8
      91441900762909683X
      \u4e0a\u6d77\u534e\u7a0b\u897f\u5357\u56fd\u9645\u65c5\u884c\u793e\u6709\u9650\u516c\u53f8
      91310105134638405A
      *\u751f\u4ea7\u751f\u6d3b\u670d\u52a1*\u4ee3\u8ba2\u673a\u7968 1 1867.924528301887 1867.92 6% 112.08
      \u4ea7\u54c1
      \u58f9\u4edf\u7396\u4f70\u634c\u62fe\u5706\u6574 \u00a51980.00
      \u97e9\u8d47\u6590
      \u00a51867.92 \u00a5112.08
      """;

    Map<String, String> fields = extractStructuredFields(invoiceText, "air-ticket.pdf");

    assertThat(fields)
      .containsEntry("total_amount", "1980.00")
      .containsEntry("amount_before_tax", "1867.92")
      .containsEntry("tax_amount", "112.08");
  }

  @Test
  void recognizeInvoiceFileReadsMeituanDeliveryPdfFields() throws Exception {
    OcrService service = new OcrService(null, null, null, null, new ObjectMapper());
    Path pdfPath = Path.of("docs", "test2", "20260727报销单", "20260727报销单",
      "上海三快智送科技有限公司_发票金额5.00元.pdf");

    Method method = OcrService.class.getDeclaredMethod("recognizeInvoiceFile", Path.class, String.class);
    method.setAccessible(true);
    String json = (String) method.invoke(service, pdfPath, pdfPath.getFileName().toString());
    JsonNode fields = new ObjectMapper().readTree(json).get("invoice_fields");

    assertThat(fields.get("invoice_no").asText()).isEqualTo("26317000002597895258");
    assertThat(fields.get("date").asText()).isEqualTo("2026年07月27日");
    assertThat(fields.get("buyer_name").asText()).isEqualTo("广东维信智联科技有限公司");
    assertThat(fields.get("buyer_tax_id").asText()).isEqualTo("91441900762909683X");
    assertThat(fields.get("seller_name").asText()).isEqualTo("上海三快智送科技有限公司");
    assertThat(fields.get("seller_tax_id").asText()).isEqualTo("91310000MA1FW9A80N");
    assertThat(fields.get("amount_before_tax").asText()).isEqualTo("4.72");
    assertThat(fields.get("tax_amount").asText()).isEqualTo("0.28");
    assertThat(fields.get("total_amount").asText()).isEqualTo("5.00");
    assertThat(fields.get("issuer").asText()).isEqualTo("何娟");
  }

  @Test
  void classifyExpenseTypeSeparatesTaxiAndPassengerTransportFromRideHailing() throws Exception {
    Invoice invoice = new Invoice();

    assertThat(classifyExpenseType(invoice, "出租车客运服务"))
      .isEqualTo("taxi");
    assertThat(classifyExpenseType(invoice, "汽车客运服务 客运车票"))
      .isEqualTo("passenger_transport");
    assertThat(classifyExpenseType(invoice, "航空运输电子客票行程单"))
      .isEqualTo("air_travel");
    assertThat(classifyExpenseType(invoice, "电子发票（铁路电子客票）"))
      .isEqualTo("rail");
  }

  @Test
  void upsertExpenseItemFromInvoiceKeepsInvoicesSeparateBeforeSupportVoucher() throws Exception {
    InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    ReimbursementExpenseItemRepository expenseItemRepository = mock(ReimbursementExpenseItemRepository.class);
    OcrService service = new OcrService(null, null, invoiceRepository, expenseItemRepository, null);
    Reimbursement reimbursement = new Reimbursement();
    reimbursement.setId(43L);
    Attachment attachment = new Attachment();
    attachment.setFileName("didi-invoice.pdf");
    attachment.setUploadedBy(7L);
    Invoice existingInvoice = invoice(1L, 43L, 81L, "12.00", LocalDateTime.of(2026, 7, 1, 0, 0));
    Invoice newInvoice = invoice(2L, 43L, null, "8.00", LocalDateTime.of(2026, 7, 1, 0, 0));
    newInvoice.setSellerName("didi");

    when(expenseItemRepository.findByInvoiceId(2L)).thenReturn(Optional.empty());
    when(expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(43L)).thenReturn(List.of(singleItem(81L, 1L)));
    when(expenseItemRepository.save(any(ReimbursementExpenseItem.class))).thenAnswer(invocation -> {
      ReimbursementExpenseItem item = invocation.getArgument(0);
      if (item.getId() == null) item.setId(82L);
      return item;
    });
    when(invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(43L)).thenReturn(List.of(existingInvoice, newInvoice));

    Method method = OcrService.class.getDeclaredMethod(
      "upsertExpenseItemFromInvoice", Reimbursement.class, Invoice.class, Attachment.class, String.class);
    method.setAccessible(true);
    ReimbursementExpenseItem saved = (ReimbursementExpenseItem) method.invoke(service, reimbursement, newInvoice, attachment, "didi");

    assertThat(saved.getId()).isEqualTo(82L);
    assertThat(saved.getInvoiceId()).isEqualTo(2L);
    assertThat(newInvoice.getExpenseItemId()).isEqualTo(82L);
    assertThat(saved.getAmount()).isEqualByComparingTo("8.00");
  }

  @Test
  void regroupExpenseItemsByInvoiceTripsMergesInvoicesWithSameVoucherTrip() {
    InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    ReimbursementExpenseItemRepository expenseItemRepository = mock(ReimbursementExpenseItemRepository.class);
    OcrService service = new OcrService(null, null, invoiceRepository, expenseItemRepository, null);
    Invoice first = invoice(1L, 43L, 81L, "12.00", LocalDateTime.of(2026, 7, 1, 0, 0));
    first.setSupportAttachmentId(101L);
    first.setStartPlace("A");
    first.setEndPlace("B");
    first.setSellerName("didi");
    Invoice second = invoice(2L, 43L, 82L, "8.00", LocalDateTime.of(2026, 7, 1, 0, 0));
    second.setSupportAttachmentId(102L);
    second.setStartPlace("A");
    second.setEndPlace("B");
    second.setSellerName("didi");
    ReimbursementExpenseItem firstItem = singleItem(81L, 1L);
    firstItem.setKilometers(new BigDecimal("18.40"));
    ReimbursementExpenseItem secondItem = singleItem(82L, 2L);
    secondItem.setKilometers(new BigDecimal("4.60"));

    when(invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(43L)).thenReturn(List.of(first, second));
    when(expenseItemRepository.findById(81L)).thenReturn(Optional.of(firstItem));
    when(expenseItemRepository.findById(82L)).thenReturn(Optional.of(secondItem));
    when(expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(43L)).thenReturn(List.of(firstItem, secondItem));
    when(expenseItemRepository.save(any(ReimbursementExpenseItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.regroupExpenseItemsByInvoiceTrips(43L, 7L);

    assertThat(first.getExpenseItemId()).isEqualTo(81L);
    assertThat(second.getExpenseItemId()).isEqualTo(81L);
    assertThat(firstItem.getInvoiceId()).isNull();
    assertThat(firstItem.getAmount()).isEqualByComparingTo("20.00");
    assertThat(firstItem.getKilometers()).isEqualByComparingTo("23.00");
    verify(expenseItemRepository).delete(secondItem);
  }

  @Test
  void regroupExpenseItemsByInvoiceTripsMergesSameDayFuelInvoicesWithoutTripSupport() {
    InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    ReimbursementExpenseItemRepository expenseItemRepository = mock(ReimbursementExpenseItemRepository.class);
    OcrService service = new OcrService(null, null, invoiceRepository, expenseItemRepository, null);
    Invoice first = invoice(1L, 43L, 81L, "30.12", LocalDateTime.of(2026, 7, 12, 0, 0));
    first.setSellerName("东莞嘉仕德石油有限公司");
    Invoice second = invoice(2L, 43L, 82L, "183.35", LocalDateTime.of(2026, 7, 12, 0, 0));
    second.setSellerName("东莞嘉仕德石油有限公司");
    ReimbursementExpenseItem firstItem = singleItem(81L, 1L);
    firstItem.setExpenseType("fuel");
    ReimbursementExpenseItem secondItem = singleItem(82L, 2L);
    secondItem.setExpenseType("fuel");

    when(invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(43L)).thenReturn(List.of(first, second));
    when(expenseItemRepository.findById(81L)).thenReturn(Optional.of(firstItem));
    when(expenseItemRepository.findById(82L)).thenReturn(Optional.of(secondItem));
    when(expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(43L)).thenReturn(List.of(firstItem, secondItem));
    when(expenseItemRepository.save(any(ReimbursementExpenseItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.regroupExpenseItemsByInvoiceTrips(43L, 7L);

    assertThat(first.getExpenseItemId()).isEqualTo(81L);
    assertThat(second.getExpenseItemId()).isEqualTo(81L);
    assertThat(firstItem.getInvoiceId()).isNull();
    assertThat(firstItem.getExpenseType()).isEqualTo("fuel");
    assertThat(firstItem.getAmount()).isEqualByComparingTo("213.47");
    verify(expenseItemRepository).delete(secondItem);
  }

  @Test
  void regroupExpenseItemsByInvoiceTripsHandlesSameTripDidiAndSameDayFuelTogether() {
    InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    ReimbursementExpenseItemRepository expenseItemRepository = mock(ReimbursementExpenseItemRepository.class);
    OcrService service = new OcrService(null, null, invoiceRepository, expenseItemRepository, null);
    Invoice didiA = invoice(1L, 43L, 81L, "15.70", LocalDateTime.of(2026, 4, 8, 0, 0));
    didiA.setInvoiceNo("26117000000530671207");
    didiA.setSellerName("北京滴滴出行科技有限公司");
    didiA.setSupportAttachmentId(101L);
    didiA.setStartPlace("汇峰中心C区");
    didiA.setEndPlace("珠岛宾馆-南门");
    Invoice didiB = invoice(2L, 43L, 82L, "17.90", LocalDateTime.of(2026, 4, 8, 0, 0));
    didiB.setInvoiceNo("26117000000531132508");
    didiB.setSellerName("北京滴滴出行科技有限公司");
    didiB.setSupportAttachmentId(102L);
    didiB.setStartPlace("汇峰中心C区");
    didiB.setEndPlace("珠岛宾馆-南门");
    Invoice fuelA = invoice(3L, 43L, 83L, "30.12", LocalDateTime.of(2026, 7, 12, 0, 0));
    fuelA.setInvoiceNo("26442000007907200231");
    fuelA.setSellerName("东莞嘉仕德石油有限公司");
    Invoice fuelB = invoice(4L, 43L, 84L, "183.35", LocalDateTime.of(2026, 7, 12, 0, 0));
    fuelB.setInvoiceNo("26442000007907223466");
    fuelB.setSellerName("东莞嘉仕德石油有限公司");
    ReimbursementExpenseItem didiItemA = singleItem(81L, 1L);
    didiItemA.setKilometers(new BigDecimal("18.40"));
    ReimbursementExpenseItem didiItemB = singleItem(82L, 2L);
    didiItemB.setKilometers(new BigDecimal("18.40"));
    ReimbursementExpenseItem fuelItemA = singleItem(83L, 3L);
    fuelItemA.setExpenseType("fuel");
    ReimbursementExpenseItem fuelItemB = singleItem(84L, 4L);
    fuelItemB.setExpenseType("fuel");

    when(invoiceRepository.findByReimbursementIdAndDeletedFalseOrderByCreatedAtAsc(43L))
      .thenReturn(List.of(didiA, didiB, fuelA, fuelB));
    when(expenseItemRepository.findById(81L)).thenReturn(Optional.of(didiItemA));
    when(expenseItemRepository.findById(82L)).thenReturn(Optional.of(didiItemB));
    when(expenseItemRepository.findById(83L)).thenReturn(Optional.of(fuelItemA));
    when(expenseItemRepository.findById(84L)).thenReturn(Optional.of(fuelItemB));
    when(expenseItemRepository.findByReimbursementIdOrderBySortOrderAscCreatedAtAsc(43L))
      .thenReturn(List.of(didiItemA, didiItemB, fuelItemA, fuelItemB));
    when(expenseItemRepository.save(any(ReimbursementExpenseItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.regroupExpenseItemsByInvoiceTrips(43L, 7L);

    assertThat(didiA.getExpenseItemId()).isEqualTo(81L);
    assertThat(didiB.getExpenseItemId()).isEqualTo(81L);
    assertThat(didiItemA.getExpenseType()).isEqualTo("ride_hailing");
    assertThat(didiItemA.getAmount()).isEqualByComparingTo("33.60");
    assertThat(didiItemA.getKilometers()).isEqualByComparingTo("36.80");
    assertThat(fuelA.getExpenseItemId()).isEqualTo(83L);
    assertThat(fuelB.getExpenseItemId()).isEqualTo(83L);
    assertThat(fuelItemA.getExpenseType()).isEqualTo("fuel");
    assertThat(fuelItemA.getAmount()).isEqualByComparingTo("213.47");
    verify(expenseItemRepository).delete(didiItemB);
    verify(expenseItemRepository).delete(fuelItemB);
  }

  private Invoice invoice(Long id, Long reimbursementId, Long expenseItemId, String totalAmount, LocalDateTime invoiceDate) {
    Invoice invoice = new Invoice();
    invoice.setId(id);
    invoice.setReimbursementId(reimbursementId);
    invoice.setExpenseItemId(expenseItemId);
    invoice.setTotalAmount(new BigDecimal(totalAmount));
    invoice.setInvoiceDate(invoiceDate);
    invoice.setDeleted(false);
    invoice.setStatus("confirmed");
    invoice.setCreatedAt(LocalDateTime.now());
    invoice.setUpdatedAt(LocalDateTime.now());
    return invoice;
  }

  private ReimbursementExpenseItem singleItem(Long id, Long invoiceId) {
    ReimbursementExpenseItem item = new ReimbursementExpenseItem();
    item.setId(id);
    item.setReimbursementId(43L);
    item.setInvoiceId(invoiceId);
    item.setExpenseType("ride_hailing");
    item.setSource("ocr");
    item.setCreatedAt(LocalDateTime.now());
    item.setUpdatedAt(LocalDateTime.now());
    return item;
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> extractStructuredFields(String text, String filename) throws Exception {
    Method method = OcrService.class.getDeclaredMethod("extractInvoiceFieldsFromText", String.class, String.class);
    method.setAccessible(true);
    return (Map<String, String>) method.invoke(ocrService, text, filename);
  }

  private String classifyExpenseType(Invoice invoice, String text) throws Exception {
    Method method = OcrService.class.getDeclaredMethod("classifyExpenseType", Invoice.class, String.class);
    method.setAccessible(true);
    return (String) method.invoke(ocrService, invoice, text);
  }
}
