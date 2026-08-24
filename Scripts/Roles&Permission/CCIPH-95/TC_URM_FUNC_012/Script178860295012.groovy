import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-95 | TC_U&RM_FUNC_012 | Edit permission and save changes

def login = new MSLogin()
def roles = new RolesPermission()

String roleName = 'Customs Manager'
if (binding.hasVariable('detailsRoleName') && detailsRoleName) {
	roleName = detailsRoleName as String
}

//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
'Navigate to Roles & Permissions and open Roles tab'
//roles.openRolesAndPermissionsFromMenu()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
roles.clickViewDetailsForRole(roleName)
roles.clickAnyPermissionOnDetailsScreen()
roles.clickEditPermissionLinkOnDetails()
roles.toggleFirstEditablePermissionOnDetails()
roles.clickSavePermissionChanges()
roles.verifyRoleDetailsPermissionsUpdated()
roles.clickCloseRoleDetails()
roles.verifyRolesAndPermissionsTitle()
WebUI.takeScreenshot()
WebUI.closeBrowser()
