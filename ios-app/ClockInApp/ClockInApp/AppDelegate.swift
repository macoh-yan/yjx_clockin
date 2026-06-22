import UIKit

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        window = UIWindow(frame: UIScreen.main.bounds)
        
        let hasToken = UserDefaults.standard.string(forKey: Constants.KEY_TOKEN) != nil
        if hasToken {
            let tabBarController = MainTabBarController()
            window?.rootViewController = tabBarController
        } else {
            let loginVC = LoginViewController()
            let navController = UINavigationController(rootViewController: loginVC)
            navController.navigationBar.isHidden = true
            window?.rootViewController = navController
        }
        
        window?.makeKeyAndVisible()
        return true
    }

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }

    func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
    }
}
