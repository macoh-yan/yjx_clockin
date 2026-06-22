import Foundation

class ApiService {
    static let BASE_URL = Constants.BASE_URL
    private static var authToken: String?
    
    static func setToken(_ token: String) {
        authToken = token
    }
    
    static func clearToken() {
        authToken = nil
    }
    
    private static func buildRequest(url: String, method: String = "GET", body: Data? = nil) -> URLRequest {
        var request = URLRequest(url: URL(string: url)!)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = authToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body = body {
            request.httpBody = body
        }
        
        return request
    }
    
    static func getJson(url: String, completion: @escaping (Any?) -> Void) {
        let request = buildRequest(url: url)
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                print("请求失败: \(error.localizedDescription)")
                completion(nil)
                return
            }
            
            guard let data = data else {
                completion(nil)
                return
            }
            
            do {
                let json = try JSONSerialization.jsonObject(with: data, options: [])
                completion(json)
            } catch {
                print("JSON解析失败: \(error.localizedDescription)")
                completion(nil)
            }
        }.resume()
    }
    
    static func login(empId: String, password: String, deviceId: String, completion: @escaping (Bool, [String: Any]?, String?) -> Void) {
        let url = "\(BASE_URL)\(Constants.ApiEndpoints.LOGIN)"
        let params: [String: Any] = [
            "emp_id": empId,
            "password": password,
            "device_id": deviceId
        ]
        
        guard let data = try? JSONSerialization.data(withJSONObject: params) else {
            completion(false, nil, nil)
            return
        }
        
        var request = URLRequest(url: URL(string: url)!)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = data
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                print("登录请求失败: \(error.localizedDescription)")
                completion(false, nil, nil)
                return
            }
            
            guard let httpResponse = response as? HTTPURLResponse else {
                completion(false, nil, nil)
                return
            }
            
            let cookie = httpResponse.allHeaderFields["Set-Cookie"] as? String
            
            guard let data = data else {
                completion(false, nil, cookie)
                return
            }
            
            do {
                if let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {
                    completion(true, json, cookie)
                } else {
                    completion(false, nil, cookie)
                }
            } catch {
                print("JSON解析失败: \(error.localizedDescription)")
                completion(false, nil, cookie)
            }
        }.resume()
    }
    
    static func getEmployeeList(completion: @escaping ([[String: String]]) -> Void) {
        let url = "\(BASE_URL)\(Constants.ApiEndpoints.EMPLOYEES)"
        getJson(url: url) { json in
            guard let json = json as? [String: Any],
                  let data = json["data"] as? [[String: Any]] else {
                completion([])
                return
            }
            
            let employees = data.compactMap { item -> [String: String]? in
                guard let empId = item["emp_id"] as? String,
                      let name = item["name"] as? String,
                      let department = item["department"] as? String else {
                    return nil
                }
                return ["emp_id": empId, "name": name, "department": department]
            }
            completion(employees)
        }
    }
    
    static func getEmployeeDetail(empId: String, completion: @escaping ([String: Any]?) -> Void) {
        let url = "\(BASE_URL)\(Constants.ApiEndpoints.EMPLOYEES)?keyword=\(empId)"
        getJson(url: url) { json in
            guard let json = json as? [String: Any],
                  let code = json["code"] as? Int,
                  code == 200,
                  let data = json["data"] as? [[String: Any]],
                  !data.isEmpty else {
                completion(nil)
                return
            }
            completion(data.first)
        }
    }
    
    static func getCurrentUser(completion: @escaping ([String: Any]) -> Void) {
        let url = "\(BASE_URL)\(Constants.ApiEndpoints.CURRENT_USER)"
        getJson(url: url) { json in
            guard let json = json as? [String: Any],
                  let code = json["code"] as? Int,
                  code == 200,
                  let data = json["data"] as? [String: Any] else {
                completion([:])
                return
            }
            completion(data)
        }
    }
    
    static func bindDevice(empId: String, deviceId: String, force: Bool = false, completion: @escaping (Bool, String?) -> Void) {
        let url = "\(BASE_URL)\(Constants.ApiEndpoints.BIND_DEVICE)"
        let params: [String: Any] = [
            "emp_id": empId,
            "device_id": deviceId,
            "force": force
        ]
        
        guard let data = try? JSONSerialization.data(withJSONObject: params) else {
            completion(false, nil)
            return
        }
        
        let request = buildRequest(url: url, method: "POST", body: data)
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(false, error.localizedDescription)
                return
            }
            
            guard let data = data else {
                completion(false, nil)
                return
            }
            
            do {
                if let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
                   let code = json["code"] as? Int,
                   code == 200 {
                    completion(true, nil)
                } else {
                    let msg = (json as? [String: Any])?["msg"] as? String ?? "绑定失败"
                    completion(false, msg)
                }
            } catch {
                completion(false, "解析失败")
            }
        }.resume()
    }
    
    static func uploadCheck(empId: String, checkType: Int, longitude: Double, latitude: Double, address: String, faceBase64: String, confirmEarly: Bool, completion: @escaping (Bool, String?) -> Void) {
        let url = "\(BASE_URL)\(Constants.ApiEndpoints.UPLOAD_CHECK)"
        let params: [String: Any] = [
            "emp_id": empId,
            "check_type": checkType,
            "longitude": longitude,
            "latitude": latitude,
            "device_id": DeviceUtils.getDeviceId(),
            "address": address,
            "wifi_mac": "",
            "face_base64": faceBase64,
            "confirm_early": confirmEarly
        ]
        
        guard let data = try? JSONSerialization.data(withJSONObject: params) else {
            completion(false, "数据序列化失败")
            return
        }
        
        let request = buildRequest(url: url, method: "POST", body: data)
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(false, error.localizedDescription)
                return
            }
            
            guard let data = data else {
                completion(false, nil)
                return
            }
            
            do {
                if let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
                   let code = json["code"] as? Int,
                   code == 200 {
                    completion(true, "打卡成功")
                } else {
                    let msg = (json as? [String: Any])?["msg"] as? String ?? "打卡失败"
                    completion(false, msg)
                }
            } catch {
                completion(false, "解析失败")
            }
        }.resume()
    }
    
    static func getDailyRecord(empId: String, completion: @escaping ([String: Any]?) -> Void) {
        let url = "\(BASE_URL)\(Constants.ApiEndpoints.DAILY_RECORD)"
        let params: [String: Any] = ["emp_id": empId]
        
        guard let data = try? JSONSerialization.data(withJSONObject: params) else {
            completion(nil)
            return
        }
        
        let request = buildRequest(url: url, method: "POST", body: data)
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                print("获取打卡记录失败: \(error.localizedDescription)")
                completion(nil)
                return
            }
            
            guard let data = data else {
                completion(nil)
                return
            }
            
            do {
                if let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
                   let code = json["code"] as? Int,
                   code == 200,
                   let data = json["data"] as? [String: Any] {
                    completion(data)
                } else {
                    completion(nil)
                }
            } catch {
                print("JSON解析失败: \(error.localizedDescription)")
                completion(nil)
            }
        }.resume()
    }
    
    static func getPunchPoints(completion: @escaping ([[String: Any]]) -> Void) {
        let url = "\(BASE_URL)\(Constants.ApiEndpoints.PUNCH_POINTS)"
        getJson(url: url) { json in
            guard let json = json as? [String: Any],
                  let code = json["code"] as? Int,
                  code == 200,
                  let data = json["data"] as? [[String: Any]] else {
                completion([])
                return
            }
            completion(data)
        }
    }
    
    static func getTimeRule(pointId: Int? = nil, completion: @escaping ([String: Any]?) -> Void) {
        var url = "\(BASE_URL)\(Constants.ApiEndpoints.TIME_RULE)"
        if let pointId = pointId {
            url += "?point_id=\(pointId)"
        }
        getJson(url: url) { json in
            guard let json = json as? [String: Any],
                  let code = json["code"] as? Int,
                  code == 200,
                  let data = json["data"] as? [String: Any] else {
                completion(nil)
                return
            }
            completion(data)
        }
    }
    
    static func changePassword(oldPassword: String, newPassword: String, completion: @escaping (Bool, String) -> Void) {
        let url = "\(BASE_URL)\(Constants.ApiEndpoints.CHANGE_PASSWORD)"
        let params: [String: Any] = [
            "old_password": oldPassword,
            "new_password": newPassword
        ]
        
        guard let data = try? JSONSerialization.data(withJSONObject: params) else {
            completion(false, "数据序列化失败")
            return
        }
        
        let request = buildRequest(url: url, method: "POST", body: data)
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(false, error.localizedDescription)
                return
            }
            
            guard let data = data else {
                completion(false, "无响应数据")
                return
            }
            
            do {
                if let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
                   let code = json["code"] as? Int,
                   code == 200 {
                    let msg = json["msg"] as? String ?? "修改成功"
                    completion(true, msg)
                } else {
                    let msg = (json as? [String: Any])?["msg"] as? String ?? "修改失败"
                    completion(false, msg)
                }
            } catch {
                completion(false, "解析失败")
            }
        }.resume()
    }
}
