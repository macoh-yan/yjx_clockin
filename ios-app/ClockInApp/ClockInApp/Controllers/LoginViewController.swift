import UIKit

class LoginViewController: UIViewController {
    
    private let empIdTextField = UITextField()
    private let passwordTextField = UITextField()
    private let loginButton = UIButton()
    private let rememberSwitch = UISwitch()
    private let rememberLabel = UILabel()
    private let errorLabel = UILabel()
    private let activityIndicator = UIActivityIndicatorView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadSavedCredentials()
    }
    
    private func setupUI() {
        view.backgroundColor = UIColor.white
        
        let logoImageView = UIImageView(image: UIImage(named: "logo"))
        logoImageView.contentMode = .scaleAspectFit
        logoImageView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(logoImageView)
        
        let titleLabel = UILabel()
        titleLabel.text = "亿杰智企考勤"
        titleLabel.font = UIFont.boldSystemFont(ofSize: 28)
        titleLabel.textColor = UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1)
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(titleLabel)
        
        empIdTextField.placeholder = "请输入员工编号"
        empIdTextField.borderStyle = .roundedRect
        empIdTextField.keyboardType = .numberPad
        empIdTextField.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(empIdTextField)
        
        passwordTextField.placeholder = "请输入密码"
        passwordTextField.borderStyle = .roundedRect
        passwordTextField.isSecureTextEntry = true
        passwordTextField.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(passwordTextField)
        
        let rememberStackView = UIStackView(arrangedSubviews: [rememberLabel, rememberSwitch])
        rememberStackView.axis = .horizontal
        rememberStackView.spacing = 8
        rememberStackView.alignment = .center
        rememberStackView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(rememberStackView)
        
        rememberLabel.text = "记住密码"
        rememberLabel.font = UIFont.systemFont(ofSize: 14)
        rememberLabel.textColor = UIColor.gray
        
        loginButton.setTitle("登录", for: .normal)
        loginButton.backgroundColor = UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1)
        loginButton.setTitleColor(.white, for: .normal)
        loginButton.layer.cornerRadius = 8
        loginButton.addTarget(self, action: #selector(loginButtonTapped), for: .touchUpInside)
        loginButton.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(loginButton)
        
        errorLabel.textColor = .red
        errorLabel.font = UIFont.systemFont(ofSize: 14)
        errorLabel.textAlignment = .center
        errorLabel.isHidden = true
        errorLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(errorLabel)
        
        activityIndicator.style = .medium
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(activityIndicator)
        
        NSLayoutConstraint.activate([
            logoImageView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 80),
            logoImageView.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            logoImageView.widthAnchor.constraint(equalToConstant: 120),
            logoImageView.heightAnchor.constraint(equalToConstant: 120),
            
            titleLabel.topAnchor.constraint(equalTo: logoImageView.bottomAnchor, constant: 20),
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            empIdTextField.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 60),
            empIdTextField.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 32),
            empIdTextField.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -32),
            empIdTextField.heightAnchor.constraint(equalToConstant: 48),
            
            passwordTextField.topAnchor.constraint(equalTo: empIdTextField.bottomAnchor, constant: 20),
            passwordTextField.leadingAnchor.constraint(equalTo: empIdTextField.leadingAnchor),
            passwordTextField.trailingAnchor.constraint(equalTo: empIdTextField.trailingAnchor),
            passwordTextField.heightAnchor.constraint(equalToConstant: 48),
            
            rememberStackView.topAnchor.constraint(equalTo: passwordTextField.bottomAnchor, constant: 16),
            rememberStackView.trailingAnchor.constraint(equalTo: empIdTextField.trailingAnchor),
            
            loginButton.topAnchor.constraint(equalTo: rememberStackView.bottomAnchor, constant: 32),
            loginButton.leadingAnchor.constraint(equalTo: empIdTextField.leadingAnchor),
            loginButton.trailingAnchor.constraint(equalTo: empIdTextField.trailingAnchor),
            loginButton.heightAnchor.constraint(equalToConstant: 50),
            
            errorLabel.topAnchor.constraint(equalTo: loginButton.bottomAnchor, constant: 20),
            errorLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            activityIndicator.centerXAnchor.constraint(equalTo: loginButton.centerXAnchor),
            activityIndicator.centerYAnchor.constraint(equalTo: loginButton.centerYAnchor)
        ])
    }
    
    private func loadSavedCredentials() {
        let defaults = UserDefaults.standard
        let isRemembered = defaults.bool(forKey: Constants.KEY_REMEMBER_PASSWORD)
        if isRemembered {
            empIdTextField.text = defaults.string(forKey: Constants.KEY_SAVED_EMP_ID)
            passwordTextField.text = defaults.string(forKey: Constants.KEY_SAVED_PASSWORD)
            rememberSwitch.isOn = true
        }
    }
    
    private func saveCredentials(empId: String, password: String) {
        let defaults = UserDefaults.standard
        if rememberSwitch.isOn {
            defaults.set(empId, forKey: Constants.KEY_SAVED_EMP_ID)
            defaults.set(password, forKey: Constants.KEY_SAVED_PASSWORD)
            defaults.set(true, forKey: Constants.KEY_REMEMBER_PASSWORD)
        } else {
            defaults.removeObject(forKey: Constants.KEY_SAVED_EMP_ID)
            defaults.removeObject(forKey: Constants.KEY_SAVED_PASSWORD)
            defaults.set(false, forKey: Constants.KEY_REMEMBER_PASSWORD)
        }
    }
    
    @objc private func loginButtonTapped() {
        guard let empId = empIdTextField.text?.trimmingCharacters(in: .whitespaces),
              let password = passwordTextField.text?.trimmingCharacters(in: .whitespaces) else {
            showError("请输入员工编号和密码")
            return
        }
        
        if empId.isEmpty {
            showError("请输入员工编号")
            return
        }
        
        if password.isEmpty {
            showError("请输入密码")
            return
        }
        
        setLoading(true)
        checkAndBindDevice(empId: empId, password: password)
    }
    
    private func checkAndBindDevice(empId: String, password: String) {
        let deviceId = DeviceUtils.getDeviceId()
        
        if !DeviceUtils.isDeviceIdValid(deviceId) {
            showError("无法获取设备ID，请检查系统设置")
            setLoading(false)
            return
        }
        
        let defaults = UserDefaults.standard
        let cachedBound = defaults.bool(forKey: Constants.KEY_DEVICE_BOUND)
        let cachedDeviceId = defaults.string(forKey: Constants.KEY_DEVICE_ID)
        
        if cachedBound && cachedDeviceId == deviceId {
            performLogin(empId: empId, password: password)
            return
        }
        
        ApiService.getEmployeeList { [weak self] employees in
            guard let self = self else { return }
            
            let currentEmp = employees.first { $0["emp_id"] == empId }
            if currentEmp == nil {
                self.showError("员工信息不存在，请检查工号")
                self.setLoading(false)
                return
            }
            
            let boundDeviceId = currentEmp?["device_id"]
            
            if boundDeviceId == nil || boundDeviceId?.isEmpty ?? true {
                ApiService.bindDevice(empId: empId, deviceId: deviceId) { [weak self] success, msg in
                    guard let self = self else { return }
                    
                    if success {
                        defaults.set(true, forKey: Constants.KEY_DEVICE_BOUND)
                        defaults.set(deviceId, forKey: Constants.KEY_DEVICE_ID)
                        self.performLogin(empId: empId, password: password)
                    } else {
                        self.showError(msg ?? "设备绑定失败，请重试")
                        self.setLoading(false)
                    }
                }
            } else if boundDeviceId == deviceId {
                defaults.set(true, forKey: Constants.KEY_DEVICE_BOUND)
                defaults.set(deviceId, forKey: Constants.KEY_DEVICE_ID)
                self.performLogin(empId: empId, password: password)
            } else {
                self.showError("该账号已绑定其他设备，请使用原绑定设备登录。如需更换设备，请联系管理员解绑。")
                self.setLoading(false)
            }
        }
    }
    
    private func performLogin(empId: String, password: String) {
        let deviceId = DeviceUtils.getDeviceId()
        
        ApiService.login(empId: empId, password: password, deviceId: deviceId) { [weak self] success, json, cookie in
            guard let self = self else { return }
            
            self.setLoading(false)
            
            if success, let json = json, let code = json["code"] as? Int, code == 200 {
                let data = json["data"] as? [String: Any]
                let token = data?["token"] as? String ?? ""
                let empIdFromData = data?["emp_id"] as? String ?? ""
                let name = data?["name"] as? String ?? ""
                
                let defaults = UserDefaults.standard
                defaults.set(token, forKey: Constants.KEY_TOKEN)
                defaults.set(empIdFromData, forKey: Constants.KEY_EMP_ID)
                defaults.set(name, forKey: Constants.KEY_EMP_NAME)
                
                if let cookie = cookie {
                    defaults.set(cookie, forKey: Constants.KEY_COOKIE)
                }
                
                self.saveCredentials(empId: empId, password: password)
                ApiService.setToken(token)
                
                let tabBarController = MainTabBarController()
                self.navigationController?.setViewControllers([tabBarController], animated: true)
            } else {
                let msg = json?["msg"] as? String ?? "登录失败，请稍后重试"
                self.showError(msg)
            }
        }
    }
    
    private func showError(_ message: String) {
        errorLabel.text = message
        errorLabel.isHidden = false
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            self.errorLabel.isHidden = true
        }
    }
    
    private func setLoading(_ loading: Bool) {
        loginButton.isEnabled = !loading
        activityIndicator.isHidden = !loading
        if loading {
            activityIndicator.startAnimating()
        } else {
            activityIndicator.stopAnimating()
        }
    }
}
