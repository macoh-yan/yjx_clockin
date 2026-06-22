import UIKit

class MainTabBarController: UITabBarController {
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupViewControllers()
        setupTabBarAppearance()
    }
    
    private func setupViewControllers() {
        let clockInVC = ClockInViewController()
        let clockInNav = UINavigationController(rootViewController: clockInVC)
        clockInNav.tabBarItem = UITabBarItem(
            title: "打卡",
            image: UIImage(named: "ic_clock"),
            selectedImage: UIImage(named: "ic_clock")
        )
        
        let workbenchVC = WorkbenchViewController()
        let workbenchNav = UINavigationController(rootViewController: workbenchVC)
        workbenchNav.tabBarItem = UITabBarItem(
            title: "工作台",
            image: UIImage(named: "ic_tab_workbench"),
            selectedImage: UIImage(named: "ic_tab_workbench")
        )
        
        let profileVC = ProfileViewController()
        let profileNav = UINavigationController(rootViewController: profileVC)
        profileNav.tabBarItem = UITabBarItem(
            title: "我的",
            image: UIImage(named: "ic_tab_profile"),
            selectedImage: UIImage(named: "ic_tab_profile")
        )
        
        viewControllers = [clockInNav, workbenchNav, profileNav]
    }
    
    private func setupTabBarAppearance() {
        tabBar.barTintColor = .white
        tabBar.tintColor = UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1)
        tabBar.unselectedItemTintColor = .gray
        
        if #available(iOS 15.0, *) {
            let appearance = UITabBarAppearance()
            appearance.configureWithOpaqueBackground()
            appearance.backgroundColor = .white
            tabBar.standardAppearance = appearance
            tabBar.scrollEdgeAppearance = appearance
        }
    }
}
