import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import rolesandpermission.BusinessActions as RolesPermission
import customsmanagement.MicrosoftLoginKeywords as MSLogin

// CSV: Roles_And_Permission_TestCases.csv | CCIPH-89 | TC_U&RM_FUNC_017 | AC8 - Clear search results

def login = new MSLogin()
def roles = new RolesPermission()

'Precondition: Login as Super Admin (handled via automation profile)'
//login.loginToCustomsManagementUsingAutomationProfile()
login.loginToStageCustomsManagementUsingAutomationProfile()
roles.openRolesAndPermissionsFromMenuStage()
roles.openRolesTab()
'Search for Finance Officer and verify filtered result'
roles.searchRoles('Finance')
roles.verifyOnlyRolesVisible(['Finance Officer'])
'Clear search and verify all roles are displayed again'
roles.clearRoleSearch()
roles.refreshRolesAndPermissionsPage()
roles.verifyAllDefaultRolesDisplayed()
WebUI.takeScreenshot()
WebUI.closeBrowser()