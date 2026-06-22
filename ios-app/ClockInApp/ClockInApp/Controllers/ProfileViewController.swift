import UIKit

class ProfileViewController: UIViewController {
    
    private let avatarImageView = UIImageView()
    private let tvUsername = UILabel()
    private let tvDepartment = UILabel()
    private let tvEmpId = UILabel()
    private let tvPhone = UILabel()
    private let tvEmail = UILabel()
    private let tvHireDate = UILabel()
    private let tvDeviceStatus = UILabel()
    
    private let btnChangePassword = UIButton()
    private let btnAbout = UIButton()
    private let btnLogout = UIButton()
    
    private var currentEmpId: String?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadUserInfoFromLocal()
        fetchUserDetail()
    }
    
    private func setupUI() {
        view.backgroundColor = UIColor(red: 0xF5/255, green: 0xF7/255, blue: 0xF9/255, alpha: 1)
        
        let headerView = UIView()
        headerView.backgroundColor = UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1)
        headerView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(headerView)
        
        avatarImageView.layer.cornerRadius = 40
        avatarImageView.clipsToBounds = true
        avatarImageView.contentMode = .scaleAspectFill
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(avatarImageView)
        
        tvUsername.font = UIFont.boldSystemFont(ofSize: 20)
        tvUsername.textColor = .white
        tvUsername.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(tvUsername)
        
        tvDepartment.font = UIFont.systemFont(ofSize: 14)
        tvDepartment.textColor = UIColor.white.withAlphaComponent(0.8)
        tvDepartment.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(tvDepartment)
        
        let infoView = UIView()
        infoView.backgroundColor = .white
        infoView.layer.cornerRadius = 12
        infoView.layer.shadowColor = UIColor.black.cgColor
        infoView.layer.shadowOpacity = 0.1
        infoView.layer.shadowOffset = CGSize(width: 0, height: 2)
        infoView.layer.shadowRadius = 4
        infoView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(infoView)
        
        let empIdLabel = UILabel()
        empIdLabel.text = "员工编号"
        empIdLabel.font = UIFont.systemFont(ofSize: 14)
        empIdLabel.textColor = .gray
        empIdLabel.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(empIdLabel)
        
        tvEmpId.font = UIFont.systemFont(ofSize: 16)
        tvEmpId.textColor = .black
        tvEmpId.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(tvEmpId)
        
        let phoneLabel = UILabel()
        phoneLabel.text = "联系电话"
        phoneLabel.font = UIFont.systemFont(ofSize: 14)
        phoneLabel.textColor = .gray
        phoneLabel.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(phoneLabel)
        
        tvPhone.font = UIFont.systemFont(ofSize: 16)
        tvPhone.textColor = .black
        tvPhone.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(tvPhone)
        
        let emailLabel = UILabel()
        emailLabel.text = "电子邮箱"
        emailLabel.font = UIFont.systemFont(ofSize: 14)
        emailLabel.textColor = .gray
        emailLabel.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(emailLabel)
        
        tvEmail.font = UIFont.systemFont(ofSize: 16)
        tvEmail.textColor = .black
        tvEmail.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(tvEmail)
        
        let hireDateLabel = UILabel()
        hireDateLabel.text = "入职日期"
        hireDateLabel.font = UIFont.systemFont(ofSize: 14)
        hireDateLabel.textColor = .gray
        hireDateLabel.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(hireDateLabel)
        
        tvHireDate.font = UIFont.systemFont(ofSize: 16)
        tvHireDate.textColor = .black
        tvHireDate.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(tvHireDate)
        
        let deviceStatusLabel = UILabel()
        deviceStatusLabel.text = "设备状态"
        deviceStatusLabel.font = UIFont.systemFont(ofSize: 14)
        deviceStatusLabel.textColor = .gray
        deviceStatusLabel.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(deviceStatusLabel)
        
        tvDeviceStatus.font = UIFont.systemFont(ofSize: 16)
        tvDeviceStatus.textColor = .black
        tvDeviceStatus.translatesAutoresizingMaskIntoConstraints = false
        infoView.addSubview(tvDeviceStatus)
        
        btnChangePassword.setTitle("修改密码", for: .normal)
        btnChangePassword.setTitleColor(UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1), for: .normal)
        btnChangePassword.backgroundColor = .white
        btnChangePassword.layer.cornerRadius = 8
        btnChangePassword.addTarget(self, action: #selector(changePasswordTapped), for: .touchUpInside)
        btnChangePassword.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(btnChangePassword)
        
        btnAbout.setTitle("关于系统", for: .normal)
        btnAbout.setTitleColor(UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1), for: .normal)
        btnAbout.backgroundColor = .white
        btnAbout.layer.cornerRadius = 8
        btnAbout.addTarget(self, action: #selector(aboutTapped), for: .touchUpInside)
        btnAbout.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(btnAbout)
        
        btnLogout.setTitle("退出登录", for: .normal)
        btnLogout.setTitleColor(.red, for: .normal)
        btnLogout.backgroundColor = .white
        btnLogout.layer.cornerRadius = 8
        btnLogout.addTarget(self, action: #selector(logoutTapped), for: .touchUpInside)
        btnLogout.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(btnLogout)
        
        NSLayoutConstraint.activate([
            headerView.topAnchor.constraint(equalTo: view.topAnchor),
            headerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            headerView.heightAnchor.constraint(equalToConstant: 220),
            
            avatarImageView.topAnchor.constraint(equalTo: headerView.topAnchor, constant: 70),
            avatarImageView.centerXAnchor.constraint(equalTo: headerView.centerXAnchor),
            avatarImageView.widthAnchor.constraint(equalToConstant: 80),
            avatarImageView.heightAnchor.constraint(equalToConstant: 80),
            
            tvUsername.topAnchor.constraint(equalTo: avatarImageView.bottomAnchor, constant: 12),
            tvUsername.centerXAnchor.constraint(equalTo: headerView.centerXAnchor),
            
            tvDepartment.topAnchor.constraint(equalTo: tvUsername.bottomAnchor, constant: 4),
            tvDepartment.centerXAnchor.constraint(equalTo: headerView.centerXAnchor),
            
            infoView.topAnchor.constraint(equalTo: headerView.bottomAnchor, constant: -30),
            infoView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            infoView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            
            empIdLabel.topAnchor.constraint(equalTo: infoView.topAnchor, constant: 20),
            empIdLabel.leadingAnchor.constraint(equalTo: infoView.leadingAnchor, constant: 16),
            
            tvEmpId.topAnchor.constraint(equalTo: empIdLabel.bottomAnchor, constant: 4),
            tvEmpId.leadingAnchor.constraint(equalTo: empIdLabel.leadingAnchor),
            
            phoneLabel.topAnchor.constraint(equalTo: tvEmpId.bottomAnchor, constant: 16),
            phoneLabel.leadingAnchor.constraint(equalTo: empIdLabel.leadingAnchor),
            
            tvPhone.topAnchor.constraint(equalTo: phoneLabel.bottomAnchor, constant: 4),
            tvPhone.leadingAnchor.constraint(equalTo: phoneLabel.leadingAnchor),
            
            emailLabel.topAnchor.constraint(equalTo: tvPhone.bottomAnchor, constant: 16),
            emailLabel.leadingAnchor.constraint(equalTo: empIdLabel.leadingAnchor),
            
            tvEmail.topAnchor.constraint(equalTo: emailLabel.bottomAnchor, constant: 4),
            tvEmail.leadingAnchor.constraint(equalTo: emailLabel.leadingAnchor),
            
            hireDateLabel.topAnchor.constraint(equalTo: tvEmail.bottomAnchor, constant: 16),
            hireDateLabel.leadingAnchor.constraint(equalTo: empIdLabel.leadingAnchor),
            
            tvHireDate.topAnchor.constraint(equalTo: hireDateLabel.bottomAnchor, constant: 4),
            tvHireDate.leadingAnchor.constraint(equalTo: hireDateLabel.leadingAnchor),
            
            deviceStatusLabel.topAnchor.constraint(equalTo: tvHireDate.bottomAnchor, constant: 16),
            deviceStatusLabel.leadingAnchor.constraint(equalTo: empIdLabel.leadingAnchor),
            
            tvDeviceStatus.topAnchor.constraint(equalTo: deviceStatusLabel.bottomAnchor, constant: 4),
            tvDeviceStatus.leadingAnchor.constraint(equalTo: deviceStatusLabel.leadingAnchor),
            tvDeviceStatus.bottomAnchor.constraint(equalTo: infoView.bottomAnchor, constant: -20),
            
            btnChangePassword.topAnchor.constraint(equalTo: infoView.bottomAnchor, constant: 20),
            btnChangePassword.leadingAnchor.constraint(equalTo: infoView.leadingAnchor),
            btnChangePassword.trailingAnchor.constraint(equalTo: infoView.trailingAnchor),
            btnChangePassword.heightAnchor.constraint(equalToConstant: 50),
            
            btnAbout.topAnchor.constraint(equalTo: btnChangePassword.bottomAnchor, constant: 12),
            btnAbout.leadingAnchor.constraint(equalTo: btnChangePassword.leadingAnchor),
            btnAbout.trailingAnchor.constraint(equalTo: btnChangePassword.trailingAnchor),
            btnAbout.heightAnchor.constraint(equalToConstant: 50),
            
            btnLogout.topAnchor.constraint(equalTo: btnAbout.bottomAnchor, constant: 12),
            btnLogout.leadingAnchor.constraint(equalTo: btnChangePassword.leadingAnchor),
            btnLogout.trailingAnchor.constraint(equalTo: btnChangePassword.trailingAnchor),
            btnLogout.heightAnchor.constraint(equalToConstant: 50)
        ])
    }
    
    private func loadUserInfoFromLocal() {
        let defaults = UserDefaults.standard
        currentEmpId = defaults.string(forKey: Constants.KEY_EMP_ID)
        
        let name = defaults.string(forKey: Constants.KEY_EMP_NAME)
        tvUsername.text = name ?? "员工"
        tvEmpId.text = currentEmpId ?? "--"
        
        tvDepartment.text = "加载中..."
        tvPhone.text = "--"
        tvEmail.text = "--"
        tvHireDate.text = "--"
        
        let deviceId = DeviceUtils.getDeviceId()
        let cachedBound = defaults.bool(forKey: Constants.KEY_DEVICE_BOUND)
        let cachedDeviceId = defaults.string(forKey: Constants.KEY_DEVICE_ID)
        tvDeviceStatus.text = (cachedBound && cachedDeviceId == deviceId) ? "已绑定" : "未绑定"
        tvDeviceStatus.textColor = (cachedBound && cachedDeviceId == deviceId) ? UIColor.systemGreen : UIColor.systemOrange
        
        avatarImageView.image = UIImage(named: "name_image")
    }
    
    private func fetchUserDetail() {
        guard let empId = currentEmpId else { return }
        
        ApiService.getEmployeeDetail(empId: empId) { [weak self] emp in
            guard let self = self else { return }
            DispatchQueue.main.async {
                if let emp = emp {
                    let name = emp["name"] as? String
                    let department = emp["department"] as? String
                    let phone = emp["phone"] as? String
                    let email = emp["email"] as? String
                    let hireDate = emp["hire_date"] as? String
                    let faceImage = emp["face_image"] as? String
                    
                    if let name = name, !name.isEmpty {
                        self.tvUsername.text = name
                    }
                    self.tvDepartment.text = department ?? "未分配"
                    self.tvPhone.text = phone ?? "--"
                    self.tvEmail.text = email ?? "--"
                    self.tvHireDate.text = hireDate ?? "--"
                    
                    if let faceImage = faceImage, !faceImage.isEmpty {
                        ImageUtils.setAvatarFromBase64(faceImage, imageView: self.avatarImageView)
                    }
                }
            }
        }
    }
    
    @objc private func changePasswordTapped() {
        let alert = UIAlertController(title: "修改密码", message: "", preferredStyle: .alert)
        
        alert.addTextField { textField in
            textField.placeholder = "原密码"
            textField.isSecureTextEntry = true
        }
        
        alert.addTextField { textField in
            textField.placeholder = "新密码"
            textField.isSecureTextEntry = true
        }
        
        alert.addTextField { textField in
            textField.placeholder = "确认新密码"
            textField.isSecureTextEntry = true
        }
        
        alert.addAction(UIAlertAction(title: "确定", style: .default) { [weak self] _ in
            guard let self = self else { return }
            
            let oldPassword = alert.textFields?[0].text ?? ""
            let newPassword = alert.textFields?[1].text ?? ""
            let confirmPassword = alert.textFields?[2].text ?? ""
            
            if oldPassword.isEmpty {
                self.showToast("请输入原密码")
                return
            }
            
            if newPassword.isEmpty {
                self.showToast("请输入新密码")
                return
            }
            
            if newPassword.count < 6 {
                self.showToast("新密码长度至少6位")
                return
            }
            
            if newPassword != confirmPassword {
                self.showToast("两次输入的新密码不一致")
                return
            }
            
            ApiService.changePassword(oldPassword: oldPassword, newPassword: newPassword) { success, msg in
                DispatchQueue.main.async {
                    if success {
                        self.showToast(msg)
                    } else {
                        self.showToast(msg)
                    }
                }
            }
        })
        
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        
        present(alert, animated: true)
    }
    
    @objc private func aboutTapped() {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let message = """
            亿杰智企综合管理平台
            版本：\(version)
            开发者：陕西亿杰鑫信息技术有限公司
            支持：智慧考勤 · 财务报销 · 采购审批 · 移动办公
            """
        
        let alert = UIAlertController(title: "关于系统", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "确定", style: .default))
        present(alert, animated: true)
    }
    
    @objc private func logoutTapped() {
        let alert = UIAlertController(title: "提示", message: "确定要退出登录吗？", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "确定", style: .destructive) { _ in
            let defaults = UserDefaults.standard
            defaults.removePersistentDomain(forName: Bundle.main.bundleIdentifier ?? "")
            ApiService.clearToken()
            
            let loginVC = LoginViewController()
            let navController = UINavigationController(rootViewController: loginVC)
            navController.navigationBar.isHidden = true
            UIApplication.shared.keyWindow?.rootViewController = navController
        })
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        present(alert, animated: true)
    }
    
    private func showToast(_ message: String) {
        let toast = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        present(toast, animated: true)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            toast.dismiss(animated: true)
        }
    }
}
