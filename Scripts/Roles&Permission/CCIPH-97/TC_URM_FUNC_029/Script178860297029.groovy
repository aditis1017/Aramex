import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-97 | TC_U&RM_FUNC_029 | AC5/AC6 - Highlight enabled permissions

def login = new MSLogin()
def roles = new RolesPermission()

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openPermissionsMatrixTab()
roles.verifyMatrixEnabledPermissionsHighlighted()
roles.verifyMatrixLimitedPermissionsForRole('Operations Staff', 'Customs', ['View'])
WebUI.takeScreenshot()
WebUI.closeBrowser()
