import UIKit
import KeychainAccess

struct DeviceUtils {
    private static let UNKNOWN_DEVICE = "unknown_device"
    
    static func getDeviceId() -> String {
        let keychain = Keychain(service: "com.example.yjx.clockin")
        
        if let existingId = keychain["device_id"] {
            return existingId
        }
        
        let uuid = UUID().uuidString
        do {
            try keychain.set(uuid, key: "device_id")
            return uuid
        } catch {
            return UNKNOWN_DEVICE
        }
    }
    
    static func isDeviceIdValid(_ deviceId: String) -> Bool {
        return !deviceId.isEmpty && deviceId != UNKNOWN_DEVICE
    }
}
