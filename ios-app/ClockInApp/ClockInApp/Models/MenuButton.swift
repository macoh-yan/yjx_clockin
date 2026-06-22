import Foundation

struct MenuButton: Codable {
    let name: String
    let icon: String
    let url: String
}

struct PunchTimeRule {
    let checkInDeadline: String
    let checkOutStart: String
    let lateMinutes: Int
    let earlyLeaveMinutes: Int
}

struct PunchRecord {
    let checkInTime: String
    let checkOutTime: String
    let status: String
}
