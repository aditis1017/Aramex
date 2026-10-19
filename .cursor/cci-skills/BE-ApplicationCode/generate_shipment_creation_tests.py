#!/usr/bin/env python3
"""Generate CCIPH-42 ShipmentCreation test cases, scripts, data CSV, and test suite."""
import csv
import os
import re
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CSV_SOURCE = ROOT / "Shipment Creation-TestCases.csv"
TC_DIR = ROOT / "Test Cases" / "ShipmentCreation" / "CCIPH-42"
SCRIPT_DIR = ROOT / "Scripts" / "ShipmentCreation" / "CCIPH-42"
DATA_CSV = ROOT / "Data Files" / "ShipmentCreation_CCIPH42_Data.csv"
DATA_DAT = ROOT / "Data Files" / "ShipmentCreation_CCIPH42_Data.dat"
SUITE_FILE = ROOT / "Test Suites" / "ShipmentCreation-TEST_SUITE.ts"

STORY_FILTER = "CCIPH-42"

DATA_COLUMNS = [
    "ID", "flow_Type", "product_Group", "shipment_Status", "account_Number",
    "charge_Group", "charge_Code", "entity", "category_Rule", "expected_Category",
    "tracking_Number", "rule_Scenario", "expected_Behavior"
]

FLOW_MAP = {
    "TC_US_FUNC_001": ("DB_ONLY", {"product_Group": "EXP", "shipment_Status": "Full"}),
    "TC_US_FUNC_002": ("DB_ONLY", {"product_Group": "DOM", "shipment_Status": "Full", "account_Number": "72104493"}),
    "TC_US_FUNC_003": ("UI", {"charge_Group": "Cost", "charge_Code": "ECD", "entity": "RUH"}),
    "TC_US_FUNC_004": ("UI", {"rule_Scenario": "RULE_1", "expected_Behavior": "RULE_1_ONLY"}),
    "TC_US_FUNC_005": ("UI", {"rule_Scenario": "RULE_2", "expected_Behavior": "RULE_2_ONLY"}),
    "TC_US_FUNC_006": ("UI", {"rule_Scenario": "RULE_3", "expected_Behavior": "RULE_3_ONLY"}),
    "TC_US_FUNC_007": ("UI", {"rule_Scenario": "RULE_1_AND_2", "expected_Behavior": "RULE_1_ONLY"}),
    "TC_US_FUNC_008": ("UI", {"rule_Scenario": "RULE_2_AND_3", "expected_Behavior": "RULE_2_ONLY"}),
    "TC_US_FUNC_009": ("UI", {"rule_Scenario": "ALL_RULES", "expected_Behavior": "RULE_1_ONLY"}),
    "TC_US_FUNC_010": ("UI", {"rule_Scenario": "RULE_1_EXPIRED", "expected_Behavior": "ALL_RECORDS"}),
    "TC_US_FUNC_011": ("UI", {"rule_Scenario": "EDIT_RULE", "expected_Behavior": "TODAYS_ONLY"}),
    "TC_US_FUNC_012": ("UI", {"rule_Scenario": "DDP_RULE_1", "expected_Behavior": "RULE_1_ONLY", "charge_Group": "Revenue", "charge_Code": "DTA"}),
    "TC_US_FUNC_013": ("UI", {"rule_Scenario": "DDP_EDIT", "expected_Behavior": "TODAYS_ONLY", "charge_Group": "Revenue", "charge_Code": "DTA"}),
    "TC_US_FUNC_014": ("UI", {"rule_Scenario": "DDP_EXPIRED", "expected_Behavior": "ALL_RECORDS", "charge_Group": "Revenue", "charge_Code": "DTA"}),
    "TC_US_FUNC_015": ("UI", {"rule_Scenario": "RULE_1", "charge_Code": "ECD,DTC"}),
    "TC_US_FUNC_016": ("UI", {"rule_Scenario": "DDP_RULE_1", "charge_Code": "ECD,DTA,ECM"}),
    "TC_US_FUNC_017": ("DB_ONLY", {"charge_Code": "ECD"}),
    "TC_US_FUNC_018": ("DB_UI", {"charge_Code": "ECD"}),
    "TC_US_FUNC_019": ("DB_ONLY", {"charge_Code": "ECD"}),
    "TC_US_FUNC_020": ("DB_UI", {"charge_Code": "ECD"}),
    "TC_US_FUNC_021": ("DB_ONLY", {"charge_Code": "DTC"}),
    "TC_US_FUNC_022": ("DB_ONLY", {"charge_Code": "DTC"}),
    "TC_US_FUNC_023": ("DB_ONLY", {"charge_Code": "DTC"}),
    "TC_US_FUNC_024": ("DB_UI", {"charge_Code": "ECD"}),
    "TC_US_FUNC_025": ("DB_UI", {"charge_Code": "ECD"}),
    "TC_US_FUNC_026": ("DB_ONLY", {"charge_Code": "DTA"}),
    "TC_US_FUNC_027": ("DB_ONLY", {"charge_Code": "DTA"}),
    "TC_US_FUNC_028": ("DB_ONLY", {"charge_Code": "DTA"}),
    "TC_US_FUNC_029": ("DB_UI", {"charge_Code": "ECM"}),
    "TC_US_FUNC_030": ("DB_UI", {"charge_Code": "ECM"}),
    "TC_US_FUNC_031": ("DB_UI", {"charge_Code": "DT3", "charge_Group": "Revenue"}),
    "TC_US_FUNC_034": ("DB_UI", {"charge_Code": "ECM"}),
    "TC_US_FUNC_037": ("UI", {}),
    "TC_US_FUNC_038": ("UI", {"expected_Behavior": "PENDING CALCULATION"}),
    "TC_US_FUNC_039": ("UI", {"expected_Behavior": "DUE"}),
    "TC_US_FUNC_040": ("UI", {"expected_Behavior": "SETTLED"}),
    "TC_US_FUNC_041": ("UI", {"expected_Behavior": "TERMINATED"}),
    "TC_US_FUNC_042": ("DB_ONLY", {"product_Group": "EXP"}),
}

CATEGORY_EXPECTED = {
    "001": "Exempted", "002": "Not Exempted", "003": "Exempted", "004": "Exempted",
    "005": "Exempted", "006": "Exempted", "007": "Not Exempted", "008": "Exempted",
    "009": "Exempted", "010": "B2B", "011": "HV", "012": "HV", "013": "LV",
    "014": "LV", "015": "HV", "016": "HV", "017": "HV", "018": "LV", "019": "LV",
    "020": "HV", "021": "HV", "022": "LV", "023": "LV", "024": "HV",
    "025": "Boundary", "026": "Exempted", "027": "Single", "028": "HV",
}


def read_cciph42_rows():
    rows = []
    with open(CSV_SOURCE, newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if (row.get("User Story ID") or "").strip() == STORY_FILTER:
                rows.append(row)
    return rows


def script_number(tc_id):
    digits = re.sub(r"\D", "", tc_id)
    return f"Script{digits}" if digits else f"Script{uuid.uuid4().hex[:9]}"


def make_tc_xml(tc_id, description, row_index):
    guid = f"sc-cciph42-{tc_id.lower().replace('_', '-')}-{row_index:04d}"
    link_id = f"sc42-link-{tc_id.lower()}"
    var_blocks = []
    for col in DATA_COLUMNS[1:]:
        var_id = f"sc42-var-{col.lower().replace('_', '-')}"
        var_blocks.append(f"""   <variable>
      <defaultValue>''</defaultValue>
      <description></description>
      <id>{var_id}</id>
      <masked>false</masked>
      <name>{col}</name>
   </variable>""")
        var_blocks.append(f"""   <variableLinks>
      <testDataLinkId>{link_id}</testDataLinkId>
      <type>DATA_COLUMN</type>
      <value>{col}</value>
      <variableId>{var_id}</variableId>
   </variableLinks>""")

    return f"""<?xml version="1.0" encoding="UTF-8"?>
<TestCaseEntity>
   <description>{description}</description>
   <name>{tc_id}</name>
   <tag>GenAI,Critical,CCIPH-42,ShipmentCreation</tag>
   <comment>CSV: Shipment Creation-TestCases.csv | Jira: https://aramex.atlassian.net/browse/CCIPH-42 | ID: {tc_id}</comment>
   <origin>GenAI</origin>
   <recordOption>OTHER</recordOption>
   <testCaseGuid>{guid}</testCaseGuid>
   <testDataLinks>
      <combinationType>ONE</combinationType>
      <id>{link_id}</id>
      <iterationEntity>
         <iterationType>SPECIFIC</iterationType>
         <value>{row_index}</value>
      </iterationEntity>
      <testDataId>Data Files/ShipmentCreation_CCIPH42_Data</testDataId>
   </testDataLinks>
{chr(10).join(var_blocks)}
</TestCaseEntity>
"""


def make_script(tc_id):
    script_name = script_number(tc_id)
    return f"""import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import ShipmentCreation.BusinessActions as SC

// CSV: Shipment Creation-TestCases.csv | CCIPH-42 | {tc_id}
// Login precondition: session established via LOGIN-TEST_SUITE or script login keyword

def sc = new SC()

try {{
    sc.executeShipmentCreationTest('{tc_id}', binding)
    WebUI.takeScreenshot()
}} catch (Exception e) {{
    WebUI.takeScreenshot()
    throw e
}} finally {{
    try {{ WebUI.closeBrowser() }} catch (Throwable ignored) {{ }}
}}
""", script_name


def data_row(tc_id, defaults):
    flow, extra = FLOW_MAP.get(tc_id, ("UI", {}))
    if tc_id.startswith("TC_CAT_FUNC_"):
        num = tc_id.replace("TC_CAT_FUNC_", "")
        flow = "UI"
        extra = {
            "category_Rule": f"GS-VC-{num.zfill(2)}",
            "expected_Category": CATEGORY_EXPECTED.get(num, "Exempted"),
            "entity": "RUH",
            "tracking_Number": "35600000001",
        }
    merged = {"ID": tc_id, "flow_Type": flow}
    for col in DATA_COLUMNS[1:]:
        merged[col] = extra.get(col, defaults.get(col, ""))
    return merged


def write_data_file(test_ids, csv_rows_by_id):
    DATA_CSV.parent.mkdir(parents=True, exist_ok=True)
    with open(DATA_CSV, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=DATA_COLUMNS, quoting=csv.QUOTE_ALL)
        writer.writeheader()
        for i, tc_id in enumerate(test_ids, start=1):
            desc_row = csv_rows_by_id.get(tc_id, {})
            writer.writerow(data_row(tc_id, desc_row))

    dat = f"""<?xml version="1.0" encoding="UTF-8"?>
<DataFileEntity>
   <description>Shipment Creation CCIPH-42 test data. Source: Data Files/ShipmentCreation_CCIPH42_Data.csv</description>
   <name>ShipmentCreation_CCIPH42_Data</name>
   <tag>CCIPH-42,ShipmentCreation</tag>
   <connectionProperties/>
   <containsHeaders>true</containsHeaders>
   <csvSeperator>COMMA</csvSeperator>
   <dataFileGUID>sc-cciph42-data-4a1b-9c0d-8e7f-000000000042</dataFileGUID>
   <dataSourceUrl>ShipmentCreation_CCIPH42_Data.csv</dataSourceUrl>
   <driver>CSV</driver>
   <isInternalPath>true</isInternalPath>
   <query></query>
   <secureUserAccount>false</secureUserAccount>
   <sheetName></sheetName>
   <usingGlobalDBSetting>false</usingGlobalDBSetting>
</DataFileEntity>
"""
    DATA_DAT.write_text(dat, encoding="utf-8")


def write_suite(test_ids):
    links = []
    for i, tc_id in enumerate(test_ids, start=1):
        guid = str(uuid.uuid4())
        links.append(f"""   <testCaseLink>
      <guid>{guid}</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ShipmentCreation/CCIPH-42/{tc_id}</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>""")

    suite = f"""<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Shipment Creation CCIPH-42 — eligibility, DDU/DDP/DDO charge creation, category rules. Chrome. Run LOGIN-TEST_SUITE first or rely on script login. Configure CCI_DB_* profile globals for DB tests. Data: Data Files/ShipmentCreation_CCIPH42_Data.csv</description>
   <name>ShipmentCreation-TEST_SUITE</name>
   <tag>CCIPH-42,ShipmentCreation,GenAI</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testSuiteGuid>sc-cciph42-suite-0001</testSuiteGuid>
{chr(10).join(links)}
</TestSuiteEntity>
"""
    SUITE_FILE.write_text(suite, encoding="utf-8")


def main():
    rows = read_cciph42_rows()
    csv_rows_by_id = {r["ID"].strip(): r for r in rows}
    test_ids = [r["ID"].strip() for r in rows]

    TC_DIR.mkdir(parents=True, exist_ok=True)
    SCRIPT_DIR.mkdir(parents=True, exist_ok=True)

    for i, row in enumerate(rows, start=1):
        tc_id = row["ID"].strip()
        description = (row.get("Test Case Description") or tc_id).replace('"', "'")
        tc_path = TC_DIR / f"{tc_id}.tc"
        tc_path.write_text(make_tc_xml(tc_id, description, i), encoding="utf-8")

        script_body, script_name = make_script(tc_id)
        script_folder = SCRIPT_DIR / tc_id
        script_folder.mkdir(parents=True, exist_ok=True)
        (script_folder / f"{script_name}.groovy").write_text(script_body, encoding="utf-8")

    write_data_file(test_ids, csv_rows_by_id)
    write_suite(test_ids)
    print(f"Generated {len(test_ids)} test cases for {STORY_FILTER}")
    print(f"  Test Cases: {TC_DIR}")
    print(f"  Scripts:    {SCRIPT_DIR}")
    print(f"  Data:       {DATA_CSV}")
    print(f"  Suite:      {SUITE_FILE}")


if __name__ == "__main__":
    main()
