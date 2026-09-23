<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Charge Creation CCIPH-77 (DTA intercompany / ASL001), CCIPH-79 (DTS/DT3 revenue at Origin), CCIPH-78 (ECS/EC3 at Origin linked to DTA), CCIPH-76 (DTC calculation), CCIPH-74 (ECD / estimates), CCIPH-121 (ECD DDO), CCIPH-122 (DTC DDO downstream), CCIPH-326 (CIF / SH013+SH267). Chrome. Run LOGIN-TEST_SUITE first. Configure CCI_DB_*, CCI_DDO_SHIPPER_NUMBER, CCI_DTA_ELIGIBLE_TRACKING, CCI_DTA_CALCULATION_API_URL, CCI_DTS_ELIGIBLE_TRACKING, CCI_DTS_PREPAID_TRACKING, CCI_DTS_ACCOUNT_TRACKING, CCI_DTS_THIRD_PARTY_TRACKING, CCI_DTS_AR_RATE_TRACKING, CCI_DTS_ENTITY_RATE_TRACKING, CCI_DTS_CALCULATION_API_URL, CCI_ECS_PREPAID_TRACKING, CCI_ECS_THIRD_PARTY_TRACKING, CCI_ECS_DTA_LINKED_TRACKING, CCI_ECS_NON_INVOICED_TRACKING, CCI_ECS_INVOICED_TRACKING, CCI_ECS_INELIGIBLE_TRACKING, CCI_ECS_ORIGIN_ENTITY, CCI_ECS_CALCULATION_API_URL, CCI_DTC_ELIGIBLE_TRACKING, CCI_DTC_CALCULATION_API_URL, CCI_ECD_PENDING_TRACKING, CCI_ECD_DUE_TRACKING, CCI_ECD_WITH_ESTIMATION_TRACKING, CCI_ECDD_ELIGIBLE_TRACKING, CCI_DOWNSTREAM_CHARGE_API_URL, CCI_CMT_COLUMN, CCI_CHARGE_SOURCE_DTA_COLUMN. Data: ChargeCreation_CCIPH77_Data.csv, ChargeCreation_CCIPH79_Data.csv, ChargeCreation_CCIPH78_Data.csv, ChargeCreation_CCIPH76_Data.csv, ChargeCreation_CCIPH74_Data.csv, ChargeCreation_CCIPH121_Data.csv, ChargeCreation_CCIPH122_Data.csv, ChargeCreation_CCIPH326_Data.csv</description>
   <name>ChargeCreation-TEST_SUITE</name>
   <tag>CCIPH-79,CCIPH-77,CCIPH-78,CCIPH-76,CCIPH-74,CCIPH-121,CCIPH-122,CCIPH-326,ChargeCreation,ECD,ECS,EC3,DTC,DTA,CIF,SH013,SH267,DDO,GenAI</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testSuiteGuid>cc-cciph121-suite-0001</testSuiteGuid>   <testCaseLink>
      <guid>cd6af0a4-7304-4e83-a36b-865b6e5e8d68</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>3dc6e285-dcdb-4f7a-9ade-494867befd55</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>607275f1-e573-4b14-a045-fddf43c0bb2a</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>406e0e2c-92d7-4b00-a10f-c409f5b788fc</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>eb7f86ee-70ea-488f-a312-6219decadc8d</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>7604d514-0d66-413d-b271-5254a5a6b16f</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>565db7c1-d180-4cd9-b237-3b642bfdc446</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>d63263d1-cf1e-4ff3-8305-66d2161c2314</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>4419ff7e-19bc-43d2-ac55-2dfbe88bbc61</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>02e7e6c5-32d7-45eb-94f4-3f3efc6cdde8</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_010</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>396b1756-0f42-42e5-89e3-898a8acae8b2</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_011</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>19436623-c53c-4083-b2fc-e60a68cd2256</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_012</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>7b4f5c80-b0ce-43ea-b4f1-7e082d419f8c</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_013</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>11390ff4-7128-49c9-b537-aa7fc94921e4</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_014</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>59da0b78-12c7-4b41-a7fe-98085fdd0c17</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_015</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>0512c68e-96e3-49c9-bf77-202e64c0f826</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_API_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>ac802f6e-9107-4832-b874-c409b61caf45</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_API_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>b7f92e9b-c0ab-4e06-aaef-861b5fa557c0</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_API_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>da022db2-853b-449c-b721-5a782c3e8cea</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_DB_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>8965095d-aaed-486b-aec3-8c84d3c62b63</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_DB_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>d021f7bd-a71c-4e21-8f55-e816f09f3e96</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_DB_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>397fa576-6bd7-4a29-bf5b-ebe79872d4cf</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_DB_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>7bf5836e-49f5-4a46-a529-90a7efcb803b</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_DB_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>5adea481-42b8-4b4b-b58b-3a4d9e6adb82</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_DB_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>b707d10b-3a1b-4a6f-8acd-7e32c5afac9b</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_DB_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>c0aaa0af-a442-4e1c-b424-25ba0858d2af</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_UI_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>67f7bc0b-2933-4a8f-a6db-e4019ee23315</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_UI_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>e841bbec-2b73-4b9e-8840-4d37feb84b92</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_UI_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>3a22a3b3-8d20-4692-bb49-79501c1fa4cd</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_PERF_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>8d731307-32cc-4f35-b8be-58f0d8db0bad</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-121/TC_ECDD_SEC_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>aea391f3-70cf-4bad-b198-bbdfdb5e1ac7</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>c919db6b-432d-427e-b0f5-10113e569090</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>fd323135-799d-4490-9f2e-0561a167dd98</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>57493335-db37-4a03-aeb7-459d729824ab</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>bdac438c-db92-475c-b339-063774ae207f</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>518c4d4d-a568-468a-bfcd-acb1804214ab</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>6a975fb4-7ccd-40a2-8035-9d93fc94adda</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>30d2b30a-5ebc-43f3-8247-14450b76cfae</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>062d4e39-9a1d-44d7-9979-fe5204a1d939</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>6cac2e86-83ec-42c9-99b5-58014e243216</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_010</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>fa382092-565e-4647-bd65-b06c5733518f</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_011</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>ad671700-56a0-49be-b708-10c6e508479f</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_012</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>d5231487-4a57-42e4-b240-f998857e4bff</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_013</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>73f498ca-7737-4329-8c04-0f1a5e02628a</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_014</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>ee21c9eb-e433-45da-80ae-aaefcd8d727b</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_015</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>5a4f0206-eef1-49e8-b719-ca60f841950e</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_016</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>08892c05-d8cb-45d6-a5d4-a4c9326fd576</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_DB_017</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>df18b66b-ae31-4a3d-8c41-50d474e7ee3d</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_DB_018</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>3dc483ca-8a4f-4d2e-b92c-270173958121</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_DB_019</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>5bcbae65-ddaf-4cd6-aa1a-e095d02d32f4</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_DB_020</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>33ecbcaa-1c27-4cdf-9a8b-3f8b14103046</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_DB_021</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>47915011-f1fa-46fe-9b03-9ec3c2aa6570</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_DB_022</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>0b3cfa31-8e5b-4905-9287-d3a5dcd14cb6</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_023</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>b173cdef-c224-4f0d-9065-d74b88edf30d</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_024</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>   <testCaseLink>
      <guid>5b7d079d-2606-4546-96ff-54e1112e1773</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-122/TC_DTC_DDO_025</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-002</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-003</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-004</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-005</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-006</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-007</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-008</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-009</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-010</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_010</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc326-suite-link-tc-cif-011</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-326/TC_CIF_011</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>b7d7afb6-ac79-4385-b41b-d3970896056e</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>5ce7aa26-43cc-46b5-b7b4-993c77a1069b</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>4298df68-b16a-40fa-b098-1d5471e80a67</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>390fa003-2880-4c7b-a213-9ba3419d0d33</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>ff65c2a8-f7dc-4f26-85ce-5b9981c47e96</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>611b55db-6491-48f1-a55d-efc1845f628b</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>e68cf899-04e9-4f08-9670-2c316a6916a4</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>f3d3c322-46d1-4e7d-b61b-4c576bb3e9ac</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_HV_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>ebaa48cb-b2c4-4d0d-9dab-7d25a66fddc0</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_HV_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>3077eae7-0a3e-4d4a-ba3d-f539278a9530</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_HV_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>2279a119-2f0b-430a-a4db-e7690421d215</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_LV_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>d5e2673a-86a3-44af-9f87-f019bc7368e1</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_LV_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>c0b71d2d-f0f4-4841-958e-edd424e76107</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_LV_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>3fd43222-6e55-4ed9-ac99-eddb5e055453</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>2e83f7d3-9636-4f86-9673-daea7afe0987</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>7411c75c-6c3d-4703-9366-86a29975506e</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>f55c8f50-8884-4685-8b3f-034e708ede0c</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>9f3e92c9-a466-4983-8fab-479604bd442d</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>4b698897-ff0b-4851-8d07-ecaa14544b21</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>65c1bf62-b165-4963-bfcf-cfa06a11ef79</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>ffcf4214-37ed-4425-99ce-0c2b42c7af5d</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>35a22101-22de-402e-8741-f6c552663a61</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_010</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>b3c7dc9b-e742-47e9-853b-2347f80d6af0</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_011</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>e643054e-4a30-449e-880d-ef2d378e3b52</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_ECD_DB_012</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>1c367871-55dd-44dc-a1cd-39efb03a3ae7</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_CURR_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>555879de-3f9e-43f1-baed-949c548d84fa</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_CURR_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>0a7aa1d2-f1bd-469b-9c63-fb0208dcca6f</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_CURR_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>dbe5a2c5-fc66-4297-bc0f-77e50709c9c5</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_CURR_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>3a42ba15-0214-48b8-8983-87db4ed1cb74</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_CURR_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>fd09833b-eb51-47d2-bc78-fd189a93d273</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_CURR_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>c6829f1e-91c6-4cd7-8165-5cf6899b7e69</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-74/TC_CURR_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-002</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-003</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-004</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-005</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-006</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-007</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-008</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-009</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-010</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_010</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-db-001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_DB_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-db-002</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_DB_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-db-003</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_DB_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-db-004</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_DB_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-db-005</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_DB_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-db-006</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_DB_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-ui-001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_UI_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc76-suite-link-tc-dtc-ui-002</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-76/TC_DTC_UI_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-002</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-003</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-004</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-005</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-006</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-007</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-008</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-009</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-010</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_010</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-011</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_011</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-012</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_012</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-013</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_013</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-014</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_014</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-015</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_015</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-db-001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_DB_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-db-002</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_DB_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-db-003</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_DB_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-db-004</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_DB_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-db-005</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_DB_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-db-006</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_DB_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-db-007</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_DB_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-db-008</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_DB_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-db-009</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_DB_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-ui-001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_UI_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc77-suite-link-tc-dta-ui-002</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-77/TC_DTA_UI_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc78-suite-link-tc-ecs-001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-78/TC_ECS_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc78-suite-link-tc-ecs-002</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-78/TC_ECS_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc78-suite-link-tc-ecs-003</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-78/TC_ECS_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc78-suite-link-tc-ecs-004</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-78/TC_ECS_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc78-suite-link-tc-ecs-005</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-78/TC_ECS_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc78-suite-link-tc-ecs-006</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-78/TC_ECS_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc78-suite-link-tc-ecs-007</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-78/TC_ECS_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc78-suite-link-tc-ecs-008</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-78/TC_ECS_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>cc78-suite-link-tc-ecs-009</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-78/TC_ECS_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>4b212db6-8deb-4a7b-884b-f7e7107adb8a</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>8bcf730c-487c-42fa-8f56-cf8e98027fc5</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>9f53a181-96ae-4218-8d5d-13faba9e6af5</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a5c4a9f0-aac1-414d-b8ac-bb82b911413a</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>191eb8d6-3538-47f0-9dc8-f4d7592735d2</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>d92c2d74-fb6b-4f34-b9fa-28fb5dcda65f</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>d708506c-3375-440d-9523-795bf8a63a38</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>b6bec091-bb94-4630-925b-c6bf01aabea9</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>c8bf976c-9d41-443d-bae6-98adee553412</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>5ef5b259-b367-4f8a-86e2-6057427e72f4</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_010</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>7e0d8d42-fcfe-4e2d-a67f-7edf45b4e47d</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_011</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>9ab76f51-5f12-4ace-9caa-d23f4f65405f</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_012</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a75998b7-3ecb-4c8a-a320-f56cecfaaed6</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_013</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>057940e1-efb4-4e10-a092-55b4448275be</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_014</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a3258fea-4513-422f-94ad-7d20a3e5bbee</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_015</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>35b69c5e-2d6a-41ff-8993-f535fed59ead</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_016</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>6a3b08dc-57cc-454b-922c-98a0538c6f53</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_017</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>8c73d8b7-0d47-44c1-a2b2-4d844c9a7625</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_018</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>307b8b13-9433-4a95-b0ee-867e6742f612</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_019</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>dddc30a8-2a27-4893-b229-2b74af93d62c</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_DB_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>3763ca1d-5b37-4502-b1f9-276d61990606</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_DB_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>29e3c3a9-3cd7-4db8-8bd1-25d39428b926</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_DB_003</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>a33db40c-c48c-4b29-b860-820da70cc9aa</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_DB_004</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>6bd05cb3-a56a-4079-ab81-a5f9122b6887</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_DB_005</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>f6f6fa77-c6e4-4680-b244-76341527d3c9</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_DB_006</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>d18034d4-6d0e-4386-8bb6-670dd14b87d5</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_DB_007</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>5d2a6108-2923-4dbd-a7d9-4ff78a544847</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_DB_008</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>001665d1-8e01-4598-a07a-ded6f9709db5</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_DB_009</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>e2ccc527-7c69-4c92-9948-b4aa429462f4</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_UI_001</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>ee432454-6d4a-47bd-8033-9fb7a06881f2</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/ChargeCreation/CCIPH-79/TC_DTS_UI_002</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
