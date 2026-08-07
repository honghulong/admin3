from docx import Document
from docx.enum.section import WD_ORIENT
from docx.enum.table import WD_ALIGN_VERTICAL, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor


OUT = r"D:\hongmengProjects\admin3\图片内容转表格.docx"


HEADERS = ["序号", "设备名称", "型号配置", "单位", "数量"]


table1 = [
    [
        "1",
        "数据服务器",
        "1. 国产化数据库服务器；\n"
        "2. CPU：不低于 2*单处理器 32 核，2.6GHz；\n"
        "3. 内存：不低于 4*DDR5 64GB；\n"
        "4. 硬盘：不低于 4*7.68TB SSD，6*16TB SATA；\n"
        "5. raid 卡：支持 RAID 0、1、5、6；\n"
        "6. 网卡：不低于 1*双口千兆网卡，2*双口万兆网卡；\n"
        "7. 其它：冗余电源。",
        "台",
        "2",
    ],
    [
        "2",
        "应用服务器",
        "1. 国产化应用服务器；\n"
        "2. CPU：不低于 2*单处理器 32 核，2.6GHz；\n"
        "3. 内存：不低于 4*DDR5 64GB；\n"
        "4. 硬盘：不低于 4*3.84TB SSD；\n"
        "5. raid 卡：支持 RAID 0、1、5、6；\n"
        "6. 网卡：不低于 1*双口千兆网卡，2*双口万兆网卡；\n"
        "7. 其它：冗余电源。",
        "台",
        "3",
    ],
    ["3", "万兆交换机", "国产化万兆交换机；\n24 端口万兆交换机，配置 24 个万兆多模光模块。", "台", "1"],
    ["4", "数据库", "国产化数据库。", "套", "2"],
    ["5", "操作系统", "国产化操作系统。", "套", "5"],
    [
        "6",
        "控制电脑",
        "采用国产 CPU，国产操作系统，核数>=8 核，主频>=2.4GHz，内存>=8GB，硬盘不低于 256GB +1TB HDD 硬盘，支持双硬盘扩展，整机一级能效认证，显示器>=23.8 英寸，国产集成显卡或独立显存>=2G 显卡。",
        "套",
        "1",
    ],
]


table2 = [
    [
        "1",
        "高清视频\n终端",
        "1. 采用国产自主编解码芯片，国产操作系统。终端主要元器件为国产自主；\n"
        "2. 支持 ITU-T H.323、IETF SIP 协议，具有良好的兼容性和开放性；\n"
        "3. 支持 64Kbps-8Mbps 呼叫带宽；\n"
        "4. 支持 H.265、H.265 SVC、H.264 HP、H.264 BP、H.264 SVC、H.263+、H.263 等图像编码协议。支持 G.711、G.722、G.722.1C、G.729A、AAC-LD、Opus 等音频协议；\n"
        "5. 支持不低于 1080P25/30fps、720P50/60fps、720P25/30fps 等分辨率。\n"
        "7. 支持 SM2、SM3、SM4 国密加密算法。",
        "台",
        "1",
    ],
    [
        "2",
        "高清摄像机",
        "1. 不低于 400 万像素，1/2.5 英寸 CMOS 成像芯片。\n"
        "2. 支持 4K30fps、1080P60fps、1080P30fps 等视频输出格式。\n"
        "3. 采用国产芯片，自主可控。",
        "台",
        "1",
    ],
    [
        "3",
        "接入交换机",
        "1. 8 口接入交换机，交换容量≥336Gbps；包转发率≥24Mpps；\n"
        "2. 安配≥8 个 10/100/1000Base-T 自适应以太网端口，4 个千兆 SFP\n"
        "3. 端口支持缓存 0.075M，保证流量突发不丢包\n"
        "4. 关键芯片（CPI、交换芯片）采用国产芯片，自主可控。",
        "台",
        "1",
    ],
    [
        "4",
        "大屏显示系统",
        "1. 产品尺寸：86 英寸，外形尺寸不小于 1967.3*87.4*1161.2mm，显示尺寸不小于 1895.04*1065.96mm；\n"
        "2. 分辨率：不低于 3840*2160。",
        "套",
        "1",
    ],
    ["5", "音频扩声及发言\n系统", "会议麦克风、音箱、调音台、功放、时序电源等", "套", "1"],
    ["6", "移动通信干扰器", "支持 5G/4G/3G/2G 信号干扰，符合保密标准。", "套", "1"],
]


def set_cell_text(cell, text, font_size=9):
    cell.text = ""
    lines = text.split("\n")
    for i, line in enumerate(lines):
        p = cell.paragraphs[0] if i == 0 else cell.add_paragraph()
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.line_spacing = 1.15
        run = p.add_run(line)
        run.font.name = "宋体"
        run._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
        run.font.size = Pt(font_size)


def shade(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def set_width(cell, width_cm):
    cell.width = Cm(width_cm)
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_w = tc_pr.first_child_found_in("w:tcW")
    if tc_w is None:
        tc_w = OxmlElement("w:tcW")
        tc_pr.append(tc_w)
    tc_w.set(qn("w:w"), str(int(width_cm / 2.54 * 1440)))
    tc_w.set(qn("w:type"), "dxa")


def add_table(doc, title, rows):
    heading = doc.add_paragraph()
    heading.paragraph_format.space_before = Pt(8)
    heading.paragraph_format.space_after = Pt(6)
    run = heading.add_run(title)
    run.bold = True
    run.font.name = "宋体"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    run.font.size = Pt(13)
    run.font.color.rgb = RGBColor(0, 0, 0)

    table = doc.add_table(rows=1, cols=len(HEADERS))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.style = "Table Grid"
    widths = [1.2, 3.0, 18.5, 1.2, 1.2]

    for idx, header in enumerate(HEADERS):
        cell = table.rows[0].cells[idx]
        set_cell_text(cell, header, 10)
        cell.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
        cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        shade(cell, "F2F2F2")
        set_width(cell, widths[idx])

    for row in rows:
        cells = table.add_row().cells
        for idx, value in enumerate(row):
            set_cell_text(cells[idx], value, 9)
            cells[idx].vertical_alignment = WD_ALIGN_VERTICAL.CENTER
            if idx in (0, 3, 4):
                for p in cells[idx].paragraphs:
                    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            set_width(cells[idx], widths[idx])

    doc.add_paragraph()


def main():
    doc = Document()
    section = doc.sections[0]
    section.orientation = WD_ORIENT.LANDSCAPE
    section.page_width = Cm(29.7)
    section.page_height = Cm(21)
    section.top_margin = Cm(1.5)
    section.bottom_margin = Cm(1.5)
    section.left_margin = Cm(1.2)
    section.right_margin = Cm(1.2)

    normal = doc.styles["Normal"]
    normal.font.name = "宋体"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    normal.font.size = Pt(10.5)

    title = doc.add_paragraph()
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title.paragraph_format.space_after = Pt(10)
    run = title.add_run("图片内容转可编辑表格")
    run.bold = True
    run.font.name = "宋体"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    run.font.size = Pt(18)

    add_table(doc, "表格一", table1)
    doc.add_page_break()
    add_table(doc, "表格二", table2)

    doc.save(OUT)


if __name__ == "__main__":
    main()
