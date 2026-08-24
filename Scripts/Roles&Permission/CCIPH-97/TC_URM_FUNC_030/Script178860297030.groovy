import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-97 | TC_U&RM_FUNC_030 | AC4 - Permission actions in matrix cells

def login = new MSLogin()
def roles = new RolesPermission()

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openPermissionsMatrixTab()
roles.verifyMatrixShowsPermissionActions('Super Admin', 'Customs')
roles.verifyMatrixLimitedPermissionsForRole('Finance Officer', 'Customs', ['View', 'Export'])
WebUI.takeScreenshot()
WebUI.closeBrowser()
