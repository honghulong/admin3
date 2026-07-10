import sys

path = "D:\\hongmengProjects\\admin3\\admin3-ui\\src\\views\\reimbursement-list.vue"
with open(path, "r", encoding="utf-8") as f:
    lines = f.readlines()

# Fix 1: Add 'any' type to row parameter in editInvoice
for i, line in enumerate(lines):
    if "function editInvoice(row)" in line and "any" not in line:
        lines[i] = line.replace("function editInvoice(row)", "function editInvoice(row: any)")
        print(f"Fixed editInvoice type at line {i+1}")
    if "function removeInvoice(index)" in line and "any" not in line:
        lines[i] = line.replace("function removeInvoice(index)", "function removeInvoice(index: number)")
        print(f"Fixed removeInvoice type at line {i+1}")

# Fix 2: Remove duplicate declarations (second occurrence of each)
seen = {}
to_remove = []
for i, line in enumerate(lines):
    stripped = line.strip()
    for var in ["const invoiceList = ref", "const invoiceEditVisible = ref", "const editingInvoiceIndex = ref", "const invoiceEditForm = reactive"]:
        if stripped.startswith(var):
            if var in seen:
                to_remove.append(i)
                print(f"Marked duplicate {var} at line {i+1}")
            else:
                seen[var] = i

for i in reversed(to_remove):
    # Remove this line and potentially following lines until next complete statement
    del lines[i]

# Fix 3: Insert formatMoney function reference - check it's defined
found_fm = any("function formatMoney" in line for line in lines)
if not found_fm:
    print("ERROR: formatMoney function not found!")

with open(path, "w", encoding="utf-8") as f:
    f.writelines(lines)
print(f"Done. Lines removed: {len(to_remove)}")
