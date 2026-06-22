import UIKit

struct WorkbenchMenuItem {
    let name: String
    let icon: UIImage?
    let url: String
    let tintColor: UIColor
}

struct PersonalGridItem {
    let name: String
    let icon: UIImage?
    let url: String
}

class WorkbenchViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    
    private let tableView = UITableView()
    private let userNameLabel = UILabel()
    
    private var personalItems: [PersonalGridItem] = []
    private var applyMenus: [WorkbenchMenuItem] = []
    private var approvalMenus: [WorkbenchMenuItem] = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadAllData()
    }
    
    private func setupUI() {
        view.backgroundColor = UIColor(red: 0xF5/255, green: 0xF7/255, blue: 0xF9/255, alpha: 1)
        
        let headerView = UIView()
        headerView.backgroundColor = UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1)
        headerView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(headerView)
        
        userNameLabel.font = UIFont.boldSystemFont(ofSize: 18)
        userNameLabel.textColor = .white
        userNameLabel.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(userNameLabel)
        
        let greetingLabel = UILabel()
        greetingLabel.text = "欢迎使用工作台"
        greetingLabel.font = UIFont.systemFont(ofSize: 14)
        greetingLabel.textColor = UIColor.white.withAlphaComponent(0.8)
        greetingLabel.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(greetingLabel)
        
        tableView.backgroundColor = .clear
        tableView.separatorStyle = .none
        tableView.dataSource = self
        tableView.delegate = self
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.register(WorkbenchSectionHeader.self, forHeaderFooterViewReuseIdentifier: "SectionHeader")
        view.addSubview(tableView)
        
        NSLayoutConstraint.activate([
            headerView.topAnchor.constraint(equalTo: view.topAnchor),
            headerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            headerView.heightAnchor.constraint(equalToConstant: 100),
            
            userNameLabel.topAnchor.constraint(equalTo: headerView.topAnchor, constant: 50),
            userNameLabel.leadingAnchor.constraint(equalTo: headerView.leadingAnchor, constant: 20),
            
            greetingLabel.topAnchor.constraint(equalTo: userNameLabel.bottomAnchor, constant: 4),
            greetingLabel.leadingAnchor.constraint(equalTo: userNameLabel.leadingAnchor),
            
            tableView.topAnchor.constraint(equalTo: headerView.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }
    
    private func loadAllData() {
        ApiService.getCurrentUser { [weak self] user in
            guard let self = self else { return }
            
            let userName = user["name"] as? String ?? "员工"
            let roles = (user["roles"] as? [String]) ?? []
            
            DispatchQueue.main.async {
                self.userNameLabel.text = "您好，\(userName)"
            }
            
            self.personalItems = [
                PersonalGridItem(name: "报销记录", icon: UIImage(named: "ic_expense"), url: "/employee/expense_list"),
                PersonalGridItem(name: "请假记录", icon: UIImage(named: "ic_leave"), url: "/employee/leave_list"),
                PersonalGridItem(name: "调休记录", icon: UIImage(named: "ic_exchange"), url: "/employee/exchange_list"),
                PersonalGridItem(name: "外出记录", icon: UIImage(named: "ic_out_apply"), url: "/employee/out_list"),
                PersonalGridItem(name: "采购记录", icon: UIImage(named: "ic_purchase"), url: "/employee/purchase_list"),
                PersonalGridItem(name: "加班记录", icon: UIImage(named: "ic_overtime"), url: "/employee/overtime_list"),
                PersonalGridItem(name: "费用申请记录", icon: UIImage(named: "ic_cost"), url: "/employee/cost_list"),
                PersonalGridItem(name: "工资条", icon: UIImage(named: "ic_salary"), url: "/employee/salary")
            ]
            
            self.applyMenus = [
                WorkbenchMenuItem(name: "请假申请", icon: UIImage(named: "ic_leave"), url: "/employee/leave_apply", tintColor: .systemBlue),
                WorkbenchMenuItem(name: "调休申请", icon: UIImage(named: "ic_exchange"), url: "/employee/exchange_apply", tintColor: .systemOrange),
                WorkbenchMenuItem(name: "报销申请", icon: UIImage(named: "ic_expense"), url: "/employee/expense_apply", tintColor: .systemGreen),
                WorkbenchMenuItem(name: "外出申请", icon: UIImage(named: "ic_out_apply"), url: "/employee/out_apply", tintColor: .systemPurple),
                WorkbenchMenuItem(name: "加班申请", icon: UIImage(named: "ic_overtime"), url: "/employee/overtime_apply", tintColor: .systemRed),
                WorkbenchMenuItem(name: "采购申请", icon: UIImage(named: "ic_purchase"), url: "/employee/purchase_apply", tintColor: .darkGray),
                WorkbenchMenuItem(name: "费用申请", icon: UIImage(named: "ic_cost"), url: "/employee/cost_apply", tintColor: .systemTeal)
            ]
            
            self.approvalMenus = self.buildApprovalMenusByRoles(roles)
            
            DispatchQueue.main.async {
                self.tableView.reloadData()
            }
        }
    }
    
    private func buildApprovalMenusByRoles(_ roles: [String]) -> [WorkbenchMenuItem] {
        let approvalRules = [
            (name: "请假审批", icon: "ic_leave_approval", url: "/admin/leave_approval", requiredRoles: ["admin", "manager", "supervisor", "general_manager", "tech_supervisor", "hr_supervisor"], tintColor: UIColor.systemBlue),
            (name: "调休审批", icon: "ic_exchange_approval", url: "/admin/exchange_approval", requiredRoles: ["admin", "manager", "supervisor", "general_manager", "tech_supervisor", "hr_supervisor"], tintColor: UIColor.systemOrange),
            (name: "外出审批", icon: "ic_out_approval", url: "/admin/out_apply", requiredRoles: ["admin", "manager", "supervisor", "general_manager", "tech_supervisor", "hr_supervisor"], tintColor: UIColor.systemPurple),
            (name: "加班审批", icon: "ic_overtime_approval", url: "/admin/overtime_approval", requiredRoles: ["admin", "manager", "supervisor", "general_manager", "tech_supervisor", "hr_supervisor"], tintColor: UIColor.systemRed),
            (name: "报销审批", icon: "ic_expense_approval", url: "/admin/expense_manage", requiredRoles: ["admin", "finance", "accountant", "general_manager", "supervisor", "manager", "tech_supervisor"], tintColor: UIColor.systemGreen),
            (name: "采购审批", icon: "ic_purchase_approval", url: "/admin/purchase_manage", requiredRoles: ["admin", "manager", "supervisor", "general_manager", "tech_supervisor", "purchaser", "storekeeper"], tintColor: UIColor.darkGray),
            (name: "费用审批", icon: "ic_cost_approval", url: "/admin/cost_approval", requiredRoles: ["admin", "finance", "accountant", "general_manager", "supervisor", "tech_supervisor"], tintColor: UIColor.systemTeal)
        ]
        
        return approvalRules.compactMap { rule in
            if rule.requiredRoles.contains(where: { roles.contains($0) }) {
                return WorkbenchMenuItem(
                    name: rule.name,
                    icon: UIImage(named: rule.icon),
                    url: rule.url,
                    tintColor: rule.tintColor
                )
            }
            return nil
        }
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        var sections = 2
        if !approvalMenus.isEmpty {
            sections += 1
        }
        return sections
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case 0:
            return 1
        case 1:
            return 1
        case 2:
            return approvalMenus.isEmpty ? 0 : 1
        default:
            return 0
        }
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
        cell.backgroundColor = .clear
        
        switch indexPath.section {
        case 0:
            let gridView = GridMenuView(items: personalItems, itemWidth: (view.frame.width - 48) / 4) { [weak self] item in
                self?.openWebPage(url: item.url, title: item.name)
            }
            gridView.translatesAutoresizingMaskIntoConstraints = false
            cell.contentView.addSubview(gridView)
            NSLayoutConstraint.activate([
                gridView.topAnchor.constraint(equalTo: cell.contentView.topAnchor),
                gridView.leadingAnchor.constraint(equalTo: cell.contentView.leadingAnchor),
                gridView.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor),
                gridView.bottomAnchor.constraint(equalTo: cell.contentView.bottomAnchor)
            ])
        case 1:
            let gridView = GridMenuView(items: applyMenus.map { menu in
                return PersonalGridItem(name: menu.name, icon: menu.icon, url: menu.url)
            }, itemWidth: (view.frame.width - 48) / 4) { [weak self] item in
                self?.openWebPage(url: item.url, title: item.name)
            }
            gridView.translatesAutoresizingMaskIntoConstraints = false
            cell.contentView.addSubview(gridView)
            NSLayoutConstraint.activate([
                gridView.topAnchor.constraint(equalTo: cell.contentView.topAnchor),
                gridView.leadingAnchor.constraint(equalTo: cell.contentView.leadingAnchor),
                gridView.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor),
                gridView.bottomAnchor.constraint(equalTo: cell.contentView.bottomAnchor)
            ])
        case 2:
            if !approvalMenus.isEmpty {
                let gridView = GridMenuView(items: approvalMenus.map { menu in
                    return PersonalGridItem(name: menu.name, icon: menu.icon, url: menu.url)
                }, itemWidth: (view.frame.width - 48) / 4) { [weak self] item in
                    self?.openWebPage(url: item.url, title: item.name)
                }
                gridView.translatesAutoresizingMaskIntoConstraints = false
                cell.contentView.addSubview(gridView)
                NSLayoutConstraint.activate([
                    gridView.topAnchor.constraint(equalTo: cell.contentView.topAnchor),
                    gridView.leadingAnchor.constraint(equalTo: cell.contentView.leadingAnchor),
                    gridView.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor),
                    gridView.bottomAnchor.constraint(equalTo: cell.contentView.bottomAnchor)
                ])
            }
        default:
            break
        }
        
        return cell
    }
    
    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        let header = tableView.dequeueReusableHeaderFooterView(withIdentifier: "SectionHeader") as? WorkbenchSectionHeader ?? WorkbenchSectionHeader()
        
        switch section {
        case 0:
            header.title = "我的记录"
        case 1:
            header.title = "申请中心"
        case 2:
            header.title = "审批中心"
        default:
            header.title = ""
        }
        
        return header
    }
    
    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 50
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        switch indexPath.section {
        case 0:
            let rows = ceil(CGFloat(personalItems.count) / 4)
            return rows * 90
        case 1:
            let rows = ceil(CGFloat(applyMenus.count) / 4)
            return rows * 90
        case 2:
            let rows = ceil(CGFloat(approvalMenus.count) / 4)
            return rows * 90
        default:
            return 0
        }
    }
    
    private func openWebPage(url: String, title: String) {
        let defaults = UserDefaults.standard
        guard let token = defaults.string(forKey: Constants.KEY_TOKEN) else {
            showLoginRequired()
            return
        }
        
        let webVC = WebViewController()
        webVC.urlString = "\(ApiService.BASE_URL)\(url)?token=\(token)"
        webVC.pageTitle = title
        navigationController?.pushViewController(webVC, animated: true)
    }
    
    private func showLoginRequired() {
        let alert = UIAlertController(title: "提示", message: "登录已过期，请重新登录", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "确定", style: .default) { _ in
            let loginVC = LoginViewController()
            let navController = UINavigationController(rootViewController: loginVC)
            navController.navigationBar.isHidden = true
            UIApplication.shared.keyWindow?.rootViewController = navController
        })
        present(alert, animated: true)
    }
}

class WorkbenchSectionHeader: UITableViewHeaderFooterView {
    var title: String = "" {
        didSet {
            titleLabel.text = title
        }
    }
    
    private let titleLabel = UILabel()
    
    override init(reuseIdentifier: String?) {
        super.init(reuseIdentifier: reuseIdentifier)
        setupViews()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupViews()
    }
    
    private func setupViews() {
        titleLabel.font = UIFont.boldSystemFont(ofSize: 16)
        titleLabel.textColor = .black
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20),
            titleLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor)
        ])
    }
}

class GridMenuView: UIView {
    private let items: [PersonalGridItem]
    private let itemWidth: CGFloat
    private let onItemClick: (PersonalGridItem) -> Void
    
    init(items: [PersonalGridItem], itemWidth: CGFloat, onItemClick: @escaping (PersonalGridItem) -> Void) {
        self.items = items
        self.itemWidth = itemWidth
        self.onItemClick = onItemClick
        super.init(frame: .zero)
        setupViews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupViews() {
        let columns = 4
        let rows = Int(ceil(CGFloat(items.count) / CGFloat(columns)))
        
        for (index, item) in items.enumerate() {
            let row = index / columns
            let col = index % columns
            
            let button = UIButton()
            button.tag = index
            button.addTarget(self, action: #selector(itemTapped(_:)), for: .touchUpInside)
            button.translatesAutoresizingMaskIntoConstraints = false
            addSubview(button)
            
            let iconView = UIImageView(image: item.icon)
            iconView.contentMode = .scaleAspectFit
            iconView.tintColor = UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1)
            iconView.translatesAutoresizingMaskIntoConstraints = false
            button.addSubview(iconView)
            
            let label = UILabel()
            label.text = item.name
            label.font = UIFont.systemFont(ofSize: 12)
            label.textColor = .darkGray
            label.textAlignment = .center
            label.numberOfLines = 0
            label.translatesAutoresizingMaskIntoConstraints = false
            button.addSubview(label)
            
            NSLayoutConstraint.activate([
                button.leadingAnchor.constraint(equalTo: leadingAnchor, constant: CGFloat(col) * itemWidth + 12),
                button.topAnchor.constraint(equalTo: topAnchor, constant: CGFloat(row) * 90 + 8),
                button.widthAnchor.constraint(equalToConstant: itemWidth - 24),
                button.heightAnchor.constraint(equalToConstant: 80),
                
                iconView.topAnchor.constraint(equalTo: button.topAnchor),
                iconView.centerXAnchor.constraint(equalTo: button.centerXAnchor),
                iconView.widthAnchor.constraint(equalToConstant: 40),
                iconView.heightAnchor.constraint(equalToConstant: 40),
                
                label.topAnchor.constraint(equalTo: iconView.bottomAnchor, constant: 4),
                label.leadingAnchor.constraint(equalTo: button.leadingAnchor),
                label.trailingAnchor.constraint(equalTo: button.trailingAnchor)
            ])
        }
        
        let height = CGFloat(rows) * 90 + 16
        heightAnchor.constraint(equalToConstant: height).isActive = true
    }
    
    @objc private func itemTapped(_ sender: UIButton) {
        let index = sender.tag
        if index < items.count {
            onItemClick(items[index])
        }
    }
}

class WebViewController: UIViewController, UIWebViewDelegate {
    var urlString: String = ""
    var pageTitle: String = ""
    
    private let webView = UIWebView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadUrl()
    }
    
    private func setupUI() {
        view.backgroundColor = .white
        
        let backButton = UIBarButtonItem(title: "返回", style: .plain, target: self, action: #selector(backButtonTapped))
        navigationItem.leftBarButtonItem = backButton
        navigationItem.title = pageTitle
        
        webView.delegate = self
        webView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(webView)
        
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }
    
    private func loadUrl() {
        if let url = URL(string: urlString) {
            let request = URLRequest(url: url)
            webView.loadRequest(request)
        }
    }
    
    @objc private func backButtonTapped() {
        if webView.canGoBack {
            webView.goBack()
        } else {
            navigationController?.popViewController(animated: true)
        }
    }
}
