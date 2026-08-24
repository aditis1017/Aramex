import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-89 | TC_U&RM_FUNC_005 | AC2 - Role card details

def login = new MSLogin()
def roles = new RolesPermission()

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Verify role card shows name, description, users assigned, and View Details'
roles.verifyRoleCardDetails('Super Admin')
WebUI.takeScreenshot()
WebUI.closeBrowser()