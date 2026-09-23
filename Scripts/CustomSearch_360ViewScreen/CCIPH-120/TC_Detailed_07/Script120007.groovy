import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import customsearch_360viewscreen.BusinessActions as CS
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: CustomSearch_360ViewScreen.csv | CCIPH-120 | TC_Detailed_07

def cs = new CS()
def login = new MSLogin()

login.loginToStageCustomsManagementUsingAutomationProfile()
cs.navigateToStageCustomsSearchScreen()
cs.verifyEcdAdditionalDetailsUI(tracking_Numbers)



cs.verifySectionAndFields(
	"GOODS SUMMARY",
	[
		"Description"    : "Car Royal Royce",
		"Collected Value": "55000.00",
		"Delivery Type"  : "LHS",
		"Services"       : "CODS"
	]
)

cs.verifySectionAndFields(
	"ORIGIN",
	[
		"Country": "AE",
		"City": "JAMKASH",
		"Entity": "DXB",
		"Shipper Name": "Saudi User"
	]
)

cs.verifySectionAndFields(
	"DESTINATION",
	[
		"Country": "SA",
		"City": "Monib MAIN CITY",
		"Entity": "RUH",
		"Consignee Name": "Advance Saudi German Trading Company",
		"Account Number": "72511436"
		
	]
)

////Goods Summary
//cs.verifySection("GOODS SUMMARY")
//cs.verifyFieldValue("Description", "Car Royal Royce")
//cs.verifyFieldValue("Collected Value", "55000.00")
//cs.verifyFieldValue("Delivery Type", "LHS")
//cs.verifyFieldValue("Services", "CODS")
//
////Origin
//cs.verifySection("ORIGIN")
//cs.verifyFieldValue("Country", "AE")
//cs.verifyFieldValue("City", "JAMKASH")
//cs.verifyFieldValue("Entity", "DXB")
//cs.verifyFieldValue("Shipper Name", "Saudi User")
//
////Destination
//cs.verifySection("DESTINATION")
//cs.verifyFieldValue("Country", "SA")
//cs.verifyFieldValue("City", "Monib MAIN CITY")
////cs.verifyFieldValue("City", "JAMKASH")
//cs.verifyFieldValue("Entity", "RUH")
//cs.verifyFieldValue("Consignee Name", "Advance Saudi German Trading Company")
//cs.verifyFieldValue("Account Number", "72511436")
//
////3RD Party
//cs.verifySection("3RD PARTY")
////cs.verifyFieldValue("Country", "GB")
//////cs.verifyFieldValue("City", "Monib MAIN CITY")
////cs.verifyFieldValue("Entity", "LON")
////cs.verifyFieldValue("Consignee Name", "Advance Saudi German Trading Company")
////cs.verifyFieldValue("Account Number", "72510683")
//
////Seller Information
//cs.verifySection("SELLER INFORMATION")
//cs.verifyFieldValue("Country", "AE")
//cs.verifyFieldValue("Entity", "DXB")
//cs.verifyFieldValue("Account Name", "Saudi User")
//cs.verifyFieldValue("Collection Reference", "C109845987")

//cs.verifyFieldValue("Collected Value", "55000.00")
//cs.verifyFieldValue("Collected Value", "55000.00")
WebUI.takeScreenshot()
WebUI.closeBrowser()
