<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Charge Creation CCIPH-77 (DTA intercompany / ASL001), CCIPH-79 (DTS/DT3 revenue at Origin), CCIPH-78 (ECS/EC3 at Origin linked to DTA), CCIPH-76 (DTC calculation), CCIPH-74 (ECD / estimates), CCIPH-121 (ECD DDO), CCIPH-122 (DTC DDO downstream), CCIPH-326 (CIF / SH013+SH267). Chrome. Run LOGIN-TEST_SUITE first. Configure CCI_DB_*, CCI_DDO_SHIPPER_NUMBER, CCI_DTA_ELIGIBLE_TRACKING, CCI_DTA_CALCULATION_API_URL, CCI_DTS_ELIGIBLE_TRACKING, CCI_DTS_PREPAID_TRACKING, CCI_DTS_ACCOUNT_TRACKING, CCI_DTS_THIRD_PARTY_TRACKING, CCI_DTS_AR_RATE_TRACKING, CCI_DTS_ENTITY_RATE_TRACKING, CCI_DTS_CALCULATION_API_URL, CCI_ECS_PREPAID_TRACKING, CCI_ECS_THIRD_PARTY_TRACKING, CCI_ECS_DTA_LINKED_TRACKING, CCI_ECS_NON_INVOICED_TRACKING, CCI_ECS_INVOICED_TRACKING, CCI_ECS_INELIGIBLE_TRACKING, CCI_ECS_ORIGIN_ENTITY, CCI_ECS_CALCULATION_API_URL, CCI_DTC_ELIGIBLE_TRACKING, CCI_DTC_CALCULATION_API_URL, CCI_ECD_PENDING_TRACKING, CCI_ECD_DUE_TRACKING, CCI_ECD_WITH_ESTIMATION_TRACKING, CCI_ECDD_ELIGIBLE_TRACKING, CCI_DOWNSTREAM_CHARGE_API_URL, CCI_CMT_COLUMN, CCI_CHARGE_SOURCE_DTA_COLUMN. Data: ChargeCreation_CCIPH77_Data.csv, ChargeCreation_CCIPH79_Data.csv, ChargeCreation_CCIPH78_Data.csv, ChargeCreation_CCIPH76_Data.csv, ChargeCreation_CCIPH74_Data.csv, ChargeCreation_CCIPH121_Data.csv, ChargeCreation_CCIPH122_Data.csv, ChargeCreation_CCIPH326_Data.csv</description>
   <name>ChargeCreation-TEST_SUITE</name>
   <tag>GenAI,CCIPH-76,CIF,CCIPH-74,CCIPH-79,CCIPH-326,CCIPH-77,ChargeCreation,CCIPH-78,ECD,CCIPH-121,CCIPH-122,SH267,SH013,DDO,DTA,DTC,ECS,EC3</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testSuiteGuid>cc-cciph121-suite-0001</testSuiteGuid>
   <testCaseLink>
      <guid>3cecf0e8-7e7f-4f70-8d49-df4e128d3651</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/ValidationPoints/TC_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>8966140e-52ac-4515-8900-bd3d671c231e</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/ValidationPoints/TC_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>ce70e167-e3d7-41f3-9b3b-82a7a5c4a1a2</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/ValidationPoints/TC_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>21bdeb6b-7d3c-4129-9e10-2207c0da967d</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/ValidationPoints/TC_ECD_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>68ac7164-424f-4bdf-b460-48d112260a6c</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/ValidationPoints/TC_ECD_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
