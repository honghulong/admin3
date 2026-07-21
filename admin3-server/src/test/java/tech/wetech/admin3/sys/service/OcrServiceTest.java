package tech.wetech.admin3.sys.service;

import org.junit.jupiter.api.Test;
import tech.wetech.admin3.sys.model.Attachment;
import tech.wetech.admin3.sys.model.Invoice;
import tech.wetech.admin3.sys.model.Reimbursement;
import tech.wetech.admin3.sys.model.ReimbursementExpenseItem;
import tech.wetech.admin3.sys.repository.InvoiceRepository;
import tech.wetech.admin3.sys.repository.ReimbursementExpenseItemRepository;

import java.lang.reflect.Method;
import java.math.BigDecimal;
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
