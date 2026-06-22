import UIKit
import MapKit

class ClockInViewController: UIViewController, CLLocationManagerDelegate, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    
    private let tvUsername = UILabel()
    private let tvDepartment = UILabel()
    private let tvCheckInTime = UILabel()
    private let tvCheckOutTime = UILabel()
    private let tvStatus = UILabel()
    private let tvDate = UILabel()
    private let btnCheckIn = UIButton()
    private let btnCheckOut = UIButton()
    private let avatarImageView = UIImageView()
    private let mapView = MKMapView()
    private let activityIndicator = UIActivityIndicatorView()
    private let errorLabel = UILabel()
    
    private var currentEmpId: String?
    private var currentEmpName: String?
    private var punchPoints: [[String: Any]] = []
    private var timeRule: [String: Any]?
    private var isClockInProgress = false
    private var pendingCheckType: Int?
    private var pendingLatitude: Double?
    private var pendingLongitude: Double?
    private var pendingAddress: String?
    
    private let locationManager = CLLocationManager()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadCurrentUserInfo()
        loadTodayCheckRecord()
        fetchPunchPoints()
        requestLocationPermission()
    }
    
    private func setupUI() {
        view.backgroundColor = UIColor(red: 0xF5/255, green: 0xF7/255, blue: 0xF9/255, alpha: 1)
        
        let headerView = UIView()
        headerView.backgroundColor = UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1)
        headerView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(headerView)
        
        tvDate.text = getTodayString()
        tvDate.font = UIFont.systemFont(ofSize: 14)
        tvDate.textColor = .white
        tvDate.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(tvDate)
        
        avatarImageView.layer.cornerRadius = 30
        avatarImageView.clipsToBounds = true
        avatarImageView.contentMode = .scaleAspectFill
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(avatarImageView)
        
        tvUsername.font = UIFont.boldSystemFont(ofSize: 18)
        tvUsername.textColor = .white
        tvUsername.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(tvUsername)
        
        tvDepartment.font = UIFont.systemFont(ofSize: 14)
        tvDepartment.textColor = UIColor.white.withAlphaComponent(0.8)
        tvDepartment.translatesAutoresizingMaskIntoConstraints = false
        headerView.addSubview(tvDepartment)
        
        mapView.showsUserLocation = true
        mapView.userTrackingMode = .followWithHeading
        mapView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(mapView)
        
        let statusView = UIView()
        statusView.backgroundColor = .white
        statusView.layer.cornerRadius = 12
        statusView.layer.shadowColor = UIColor.black.cgColor
        statusView.layer.shadowOpacity = 0.1
        statusView.layer.shadowOffset = CGSize(width: 0, height: 2)
        statusView.layer.shadowRadius = 4
        statusView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(statusView)
        
        let statusTitleLabel = UILabel()
        statusTitleLabel.text = "今日状态"
        statusTitleLabel.font = UIFont.boldSystemFont(ofSize: 16)
        statusTitleLabel.translatesAutoresizingMaskIntoConstraints = false
        statusView.addSubview(statusTitleLabel)
        
        tvStatus.font = UIFont.boldSystemFont(ofSize: 24)
        tvStatus.textColor = UIColor(red: 0x25/255, green: 0x63/255, blue: 0xEB/255, alpha: 1)
        tvStatus.textAlignment = .center
        tvStatus.translatesAutoresizingMaskIntoConstraints = false
        statusView.addSubview(tvStatus)
        
        let checkInLabel = UILabel()
        checkInLabel.text = "上班时间"
        checkInLabel.font = UIFont.systemFont(ofSize: 14)
        checkInLabel.textColor = .gray
        checkInLabel.translatesAutoresizingMaskIntoConstraints = false
        statusView.addSubview(checkInLabel)
        
        tvCheckInTime.font = UIFont.systemFont(ofSize: 16)
        tvCheckInTime.textColor = .black
        tvCheckInTime.translatesAutoresizingMaskIntoConstraints = false
        statusView.addSubview(tvCheckInTime)
        
        let checkOutLabel = UILabel()
        checkOutLabel.text = "下班时间"
        checkOutLabel.font = UIFont.systemFont(ofSize: 14)
        checkOutLabel.textColor = .gray
        checkOutLabel.translatesAutoresizingMaskIntoConstraints = false
        statusView.addSubview(checkOutLabel)
        
        tvCheckOutTime.font = UIFont.systemFont(ofSize: 16)
        tvCheckOutTime.textColor = .black
        tvCheckOutTime.translatesAutoresizingMaskIntoConstraints = false
        statusView.addSubview(tvCheckOutTime)
        
        btnCheckIn.setTitle("上班打卡", for: .normal)
        btnCheckIn.backgroundColor = UIColor(red: 0x22/255, green: 0xC5/255, blue: 0x5E/255, alpha: 1)
        btnCheckIn.setTitleColor(.white, for: .normal)
        btnCheckIn.layer.cornerRadius = 8
        btnCheckIn.addTarget(self, action: #selector(checkInButtonTapped), for: .touchUpInside)
        btnCheckIn.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(btnCheckIn)
        
        btnCheckOut.setTitle("下班打卡", for: .normal)
        btnCheckOut.backgroundColor = UIColor(red: 0xF9/255, green: 0x73/255, blue: 0x16/255, alpha: 1)
        btnCheckOut.setTitleColor(.white, for: .normal)
        btnCheckOut.layer.cornerRadius = 8
        btnCheckOut.addTarget(self, action: #selector(checkOutButtonTapped), for: .touchUpInside)
        btnCheckOut.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(btnCheckOut)
        
        errorLabel.textColor = .red
        errorLabel.font = UIFont.systemFont(ofSize: 14)
        errorLabel.textAlignment = .center
        errorLabel.isHidden = true
        errorLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(errorLabel)
        
        activityIndicator.style = .large
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(activityIndicator)
        
        NSLayoutConstraint.activate([
            headerView.topAnchor.constraint(equalTo: view.topAnchor),
            headerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            headerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            headerView.heightAnchor.constraint(equalToConstant: 200),
            
            tvDate.topAnchor.constraint(equalTo: headerView.topAnchor, constant: 60),
            tvDate.centerXAnchor.constraint(equalTo: headerView.centerXAnchor),
            
            avatarImageView.topAnchor.constraint(equalTo: tvDate.bottomAnchor, constant: 16),
            avatarImageView.centerXAnchor.constraint(equalTo: headerView.centerXAnchor),
            avatarImageView.widthAnchor.constraint(equalToConstant: 60),
            avatarImageView.heightAnchor.constraint(equalToConstant: 60),
            
            tvUsername.topAnchor.constraint(equalTo: avatarImageView.bottomAnchor, constant: 8),
            tvUsername.centerXAnchor.constraint(equalTo: headerView.centerXAnchor),
            
            tvDepartment.topAnchor.constraint(equalTo: tvUsername.bottomAnchor, constant: 4),
            tvDepartment.centerXAnchor.constraint(equalTo: headerView.centerXAnchor),
            
            mapView.topAnchor.constraint(equalTo: headerView.bottomAnchor, constant: 16),
            mapView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            mapView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            mapView.heightAnchor.constraint(equalToConstant: 200),
            
            statusView.topAnchor.constraint(equalTo: mapView.bottomAnchor, constant: 16),
            statusView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            statusView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            
            statusTitleLabel.topAnchor.constraint(equalTo: statusView.topAnchor, constant: 16),
            statusTitleLabel.leadingAnchor.constraint(equalTo: statusView.leadingAnchor, constant: 16),
            
            tvStatus.topAnchor.constraint(equalTo: statusTitleLabel.bottomAnchor, constant: 12),
            tvStatus.centerXAnchor.constraint(equalTo: statusView.centerXAnchor),
            
            checkInLabel.topAnchor.constraint(equalTo: tvStatus.bottomAnchor, constant: 16),
            checkInLabel.leadingAnchor.constraint(equalTo: statusView.leadingAnchor, constant: 32),
            
            tvCheckInTime.topAnchor.constraint(equalTo: checkInLabel.bottomAnchor, constant: 4),
            tvCheckInTime.leadingAnchor.constraint(equalTo: checkInLabel.leadingAnchor),
            
            checkOutLabel.topAnchor.constraint(equalTo: checkInLabel.topAnchor),
            checkOutLabel.trailingAnchor.constraint(equalTo: statusView.trailingAnchor, constant: -32),
            
            tvCheckOutTime.topAnchor.constraint(equalTo: checkOutLabel.bottomAnchor, constant: 4),
            tvCheckOutTime.trailingAnchor.constraint(equalTo: checkOutLabel.trailingAnchor),
            
            btnCheckIn.topAnchor.constraint(equalTo: statusView.bottomAnchor, constant: 24),
            btnCheckIn.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 32),
            btnCheckIn.widthAnchor.constraint(equalToConstant: 140),
            btnCheckIn.heightAnchor.constraint(equalToConstant: 50),
            
            btnCheckOut.topAnchor.constraint(equalTo: btnCheckIn.topAnchor),
            btnCheckOut.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -32),
            btnCheckOut.widthAnchor.constraint(equalToConstant: 140),
            btnCheckOut.heightAnchor.constraint(equalToConstant: 50),
            
            errorLabel.topAnchor.constraint(equalTo: btnCheckIn.bottomAnchor, constant: 16),
            errorLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            activityIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            activityIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
    }
    
    private func getTodayString() -> String {
        let calendar = Calendar.current
        let year = calendar.component(.year, from: Date())
        let month = calendar.component(.month, from: Date())
        let day = calendar.component(.day, from: Date())
        let weekDay = calendar.component(.weekday, from: Date())
        let weekDays = ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"]
        return "今天是 \(year) 年 \(month) 月 \(day) 日 \(weekDays[weekDay - 1])"
    }
    
    private func requestLocationPermission() {
        locationManager.delegate = self
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.last {
            updateUserLocation(location)
        }
    }
    
    private func updateUserLocation(_ location: CLLocation) {
        let region = MKCoordinateRegion(center: location.coordinate, span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01))
        mapView.setRegion(region, animated: true)
    }
    
    private func loadCurrentUserInfo() {
        let defaults = UserDefaults.standard
        currentEmpId = defaults.string(forKey: Constants.KEY_EMP_ID)
        currentEmpName = defaults.string(forKey: Constants.KEY_EMP_NAME)
        
        if let name = currentEmpName {
            tvUsername.text = name
        } else {
            tvUsername.text = "未登录"
        }
        
        tvDepartment.text = "部门：加载中..."
        
        if let empId = currentEmpId {
            ApiService.getEmployeeDetail(empId: empId) { [weak self] emp in
                guard let self = self else { return }
                DispatchQueue.main.async {
                    if let emp = emp {
                        let department = emp["department"] as? String ?? "未分配"
                        self.tvDepartment.text = "部门：\(department)"
                        
                        if let faceImage = emp["face_image"] as? String, !faceImage.isEmpty {
                            ImageUtils.setAvatarFromBase64(faceImage, imageView: self.avatarImageView)
                        } else {
                            self.avatarImageView.image = UIImage(named: "name_image")
                        }
                    }
                }
            }
        }
    }
    
    private func loadTodayCheckRecord() {
        guard let empId = currentEmpId else { return }
        
        ApiService.getDailyRecord(empId: empId) { [weak self] data in
            guard let self = self else { return }
            DispatchQueue.main.async {
                if let data = data {
                    let checkInTime = data["check_in_time"] as? String ?? "未打卡"
                    let checkOutTime = data["check_out_time"] as? String ?? "未打卡"
                    
                    self.tvCheckInTime.text = checkInTime
                    self.tvCheckOutTime.text = checkOutTime
                    
                    let status: String
                    if checkInTime != "未打卡" && checkOutTime != "未打卡" {
                        status = "已完成"
                    } else if checkInTime != "未打卡" && checkOutTime == "未打卡" {
                        status = "已上班，未下班"
                    } else {
                        status = "未打卡"
                    }
                    self.tvStatus.text = status
                    
                    self.btnCheckIn.isEnabled = checkInTime == "未打卡"
                    self.btnCheckOut.isEnabled = checkInTime != "未打卡" && checkOutTime == "未打卡"
                }
            }
        }
    }
    
    private func fetchPunchPoints() {
        ApiService.getPunchPoints { [weak self] points in
            guard let self = self else { return }
            self.punchPoints = points
            DispatchQueue.main.async {
                self.drawPunchPointsOnMap()
            }
        }
    }
    
    private func drawPunchPointsOnMap() {
        for point in punchPoints {
            guard let lat = point["latitude"] as? Double,
                  let lng = point["longitude"] as? Double,
                  let radius = point["radius"] as? Int else { continue }
            
            let center = CLLocationCoordinate2D(latitude: lat, longitude: lng)
            
            let circle = MKCircle(center: center, radius: CLLocationDistance(radius))
            mapView.addOverlay(circle)
            
            let annotation = MKPointAnnotation()
            annotation.coordinate = center
            annotation.title = point["name"] as? String
            mapView.addAnnotation(annotation)
        }
    }
    
    @objc private func checkInButtonTapped() {
        if isClockInProgress {
            showDialog(title: "提示", message: "打卡处理中，请稍后...")
            return
        }
        
        guard let empId = currentEmpId else {
            showDialog(title: "提示", message: "员工信息缺失")
            return
        }
        
        isClockInProgress = true
        
        ensureDeviceBindingAndProceed { [weak self] in
            guard let self = self else { return }
            self.getCurrentLocation { lat, lng, addr in
                if let lat = lat, let lng = lng {
                    if self.isFaceRequiredAtLocation(lat: lat, lng: lng) {
                        self.pendingCheckType = 1
                        self.pendingLatitude = lat
                        self.pendingLongitude = lng
                        self.pendingAddress = addr
                        self.openCamera()
                    } else {
                        self.submitCheck(checkType: 1, latitude: lat, longitude: lng, address: addr ?? "", faceBase64: "", confirmEarly: false)
                    }
                } else {
                    self.showDialog(title: "提示", message: "无法获取位置，请检查定位权限")
                    self.isClockInProgress = false
                }
            }
        } onError: { [weak self] msg in
            self?.showDialog(title: "提示", message: msg)
            self?.isClockInProgress = false
        }
    }
    
    @objc private func checkOutButtonTapped() {
        if isClockInProgress {
            showDialog(title: "提示", message: "打卡处理中，请稍后...")
            return
        }
        
        guard let empId = currentEmpId else {
            showDialog(title: "提示", message: "员工信息缺失")
            return
        }
        
        isClockInProgress = true
        
        ensureDeviceBindingAndProceed { [weak self] in
            guard let self = self else { return }
            self.getCurrentLocation { lat, lng, addr in
                if let lat = lat, let lng = lng {
                    if self.isFaceRequiredAtLocation(lat: lat, lng: lng) {
                        self.pendingCheckType = 2
                        self.pendingLatitude = lat
                        self.pendingLongitude = lng
                        self.pendingAddress = addr
                        self.openCamera()
                    } else {
                        self.checkEarlyLeaveAndSubmit(checkType: 2, latitude: lat, longitude: lng, address: addr ?? "")
                    }
                } else {
                    self.showDialog(title: "提示", message: "无法获取位置，请检查定位权限")
                    self.isClockInProgress = false
                }
            }
        } onError: { [weak self] msg in
            self?.showDialog(title: "提示", message: msg)
            self?.isClockInProgress = false
        }
    }
    
    private func checkEarlyLeaveAndSubmit(checkType: Int, latitude: Double, longitude: Double, address: String) {
        if timeRule == nil {
            ApiService.getTimeRule { [weak self] rule in
                self?.timeRule = rule
                self?.submitEarlyLeaveCheck(checkType: checkType, latitude: latitude, longitude: longitude, address: address)
            }
        } else {
            submitEarlyLeaveCheck(checkType: checkType, latitude: latitude, longitude: longitude, address: address)
        }
    }
    
    private func submitEarlyLeaveCheck(checkType: Int, latitude: Double, longitude: Double, address: String) {
        if isEarlyLeave() {
            showEarlyLeaveConfirmDialog { [weak self] confirmed in
                if confirmed {
                    self?.submitCheck(checkType: checkType, latitude: latitude, longitude: longitude, address: address, faceBase64: "", confirmEarly: true)
                } else {
                    self?.isClockInProgress = false
                }
            }
        } else {
            submitCheck(checkType: checkType, latitude: latitude, longitude: longitude, address: address, faceBase64: "", confirmEarly: false)
        }
    }
    
    private func isEarlyLeave() -> Bool {
        guard let rule = timeRule, let checkOutStart = rule["check_out_start"] as? String else {
            return false
        }
        
        let parts = checkOutStart.components(separatedBy: ":")
        guard parts.count == 2, let hour = Int(parts[0]), let minute = Int(parts[1]) else {
            return false
        }
        
        let calendar = Calendar.current
        let nowHour = calendar.component(.hour, from: Date())
        let nowMinute = calendar.component(.minute, from: Date())
        let nowTotalMinutes = nowHour * 60 + nowMinute
        let ruleTotalMinutes = hour * 60 + minute
        
        return nowTotalMinutes < ruleTotalMinutes
    }
    
    private func isFaceRequiredAtLocation(lat: Double, lng: Double) -> Bool {
        for point in punchPoints {
            guard let pointLat = point["latitude"] as? Double,
                  let pointLng = point["longitude"] as? Double,
                  let radius = point["radius"] as? Int else { continue }
            
            let distance = calculateDistance(lat1: lat, lng1: lng, lat2: pointLat, lng2: pointLng)
            if distance <= Double(radius) {
                return point["face_required"] as? Int == 1
            }
        }
        return false
    }
    
    private func calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double) -> Double {
        let r = 6371000.0
        let dLat = (lat2 - lat1).toRadians()
        let dLng = (lng2 - lng1).toRadians()
        let a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1.toRadians()) * cos(lat2.toRadians()) *
                sin(dLng / 2) * sin(dLng / 2)
        let c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
    
    private func getCurrentLocation(completion: @escaping (Double?, Double?, String?) -> Void) {
        if let location = locationManager.location {
            let geocoder = CLGeocoder()
            geocoder.reverseGeocodeLocation(location) { placemarks, error in
                let address = placemarks?.first?.locality ?? ""
                completion(location.coordinate.latitude, location.coordinate.longitude, address)
            }
        } else {
            completion(nil, nil, nil)
        }
    }
    
    private func ensureDeviceBindingAndProceed(onComplete: @escaping () -> Void, onError: @escaping (String) -> Void) {
        let deviceId = DeviceUtils.getDeviceId()
        
        if !DeviceUtils.isDeviceIdValid(deviceId) {
            onError("无法获取设备ID")
            return
        }
        
        guard let empId = currentEmpId else {
            onError("员工信息缺失")
            return
        }
        
        let defaults = UserDefaults.standard
        let cachedBound = defaults.bool(forKey: Constants.KEY_DEVICE_BOUND)
        let cachedDeviceId = defaults.string(forKey: Constants.KEY_DEVICE_ID)
        
        if cachedBound && cachedDeviceId == deviceId {
            onComplete()
            return
        }
        
        ApiService.getEmployeeList { [weak self] employees in
            guard let self = self else { return }
            
            let currentEmp = employees.first { $0["emp_id"] == empId }
            if currentEmp == nil {
                onError("员工信息不存在")
                return
            }
            
            let boundDeviceId = currentEmp?["device_id"]
            
            if boundDeviceId == nil || boundDeviceId?.isEmpty ?? true {
                ApiService.bindDevice(empId: empId, deviceId: deviceId) { success, msg in
                    if success {
                        defaults.set(true, forKey: Constants.KEY_DEVICE_BOUND)
                        defaults.set(deviceId, forKey: Constants.KEY_DEVICE_ID)
                        onComplete()
                    } else {
                        onError(msg ?? "设备绑定失败")
                    }
                }
            } else if boundDeviceId == deviceId {
                defaults.set(true, forKey: Constants.KEY_DEVICE_BOUND)
                defaults.set(deviceId, forKey: Constants.KEY_DEVICE_ID)
                onComplete()
            } else {
                onError("该账号已绑定其他设备，请使用原绑定设备打卡")
            }
        }
    }
    
    private func openCamera() {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = self
        present(picker, animated: true)
    }
    
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        picker.dismiss(animated: true)
        
        if let image = info[.originalImage] as? UIImage {
            let base64 = ImageUtils.imageToBase64(image)
            
            if let checkType = pendingCheckType,
               let lat = pendingLatitude,
               let lng = pendingLongitude {
                
                if checkType == 2 && isEarlyLeave() {
                    showEarlyLeaveConfirmDialog { [weak self] confirmed in
                        if confirmed {
                            self?.submitCheck(checkType: checkType, latitude: lat, longitude: lng, address: self?.pendingAddress ?? "", faceBase64: base64, confirmEarly: true)
                        } else {
                            self?.isClockInProgress = false
                        }
                    }
                } else {
                    submitCheck(checkType: checkType, latitude: lat, longitude: lng, address: pendingAddress ?? "", faceBase64: base64, confirmEarly: false)
                }
            }
        }
        
        pendingCheckType = nil
        pendingLatitude = nil
        pendingLongitude = nil
        pendingAddress = nil
    }
    
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)
        isClockInProgress = false
        pendingCheckType = nil
        pendingLatitude = nil
        pendingLongitude = nil
        pendingAddress = nil
    }
    
    private func submitCheck(checkType: Int, latitude: Double, longitude: Double, address: String, faceBase64: String, confirmEarly: Bool) {
        guard let empId = currentEmpId else {
            showDialog(title: "提示", message: "员工信息缺失")
            isClockInProgress = false
            return
        }
        
        activityIndicator.startAnimating()
        
        ApiService.uploadCheck(empId: empId, checkType: checkType, longitude: longitude, latitude: latitude, address: address, faceBase64: faceBase64, confirmEarly: confirmEarly) { [weak self] success, msg in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                self.activityIndicator.stopAnimating()
                
                if success {
                    self.showDialog(title: "打卡成功", message: msg ?? "打卡成功")
                    self.loadTodayCheckRecord()
                } else {
                    self.showDialog(title: "打卡失败", message: msg ?? "打卡失败")
                }
                
                self.isClockInProgress = false
            }
        }
    }
    
    private func showEarlyLeaveConfirmDialog(completion: @escaping (Bool) -> Void) {
        let alert = UIAlertController(title: "早退确认", message: "当前时间未到下班时间，是否确认打卡？（将记为早退）", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "确认早退", style: .destructive) { _ in
            completion(true)
        })
        alert.addAction(UIAlertAction(title: "取消", style: .cancel) { _ in
            completion(false)
        })
        present(alert, animated: true)
    }
    
    private func showDialog(title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "确定", style: .default))
        present(alert, animated: true)
    }
}

extension Double {
    func toRadians() -> Double {
        return self * .pi / 180
    }
}

extension MKMapView {
    func addCircleOverlay(center: CLLocationCoordinate2D, radius: CLLocationDistance) {
        let circle = MKCircle(center: center, radius: radius)
        addOverlay(circle)
    }
}
