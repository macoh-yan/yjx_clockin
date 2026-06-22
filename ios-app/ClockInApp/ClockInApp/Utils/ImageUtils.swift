import UIKit

struct ImageUtils {
    static func base64ToImage(_ base64Str: String) -> UIImage? {
        var pureBase64 = base64Str
        if let range = base64Str.range(of: "base64,") {
            pureBase64 = String(base64Str[range.upperBound...])
        }
        
        guard let data = Data(base64Encoded: pureBase64) else {
            return nil
        }
        return UIImage(data: data)
    }
    
    static func setAvatarFromBase64(_ base64Str: String, imageView: UIImageView, defaultImage: UIImage? = nil) {
        if let image = base64ToImage(base64Str) {
            imageView.image = image
        } else {
            imageView.image = defaultImage ?? UIImage(named: "name_image")
        }
    }
    
    static func imageToBase64(_ image: UIImage, quality: CGFloat = 0.7, maxWidth: CGFloat = 480, maxHeight: CGFloat = 640) -> String {
        let scaledImage = scaleImage(image, maxWidth: maxWidth, maxHeight: maxHeight)
        guard let data = scaledImage.jpegData(compressionQuality: quality) else {
            return ""
        }
        return data.base64EncodedString()
    }
    
    static func scaleImage(_ image: UIImage, maxWidth: CGFloat, maxHeight: CGFloat) -> UIImage {
        let width = image.size.width
        let height = image.size.height
        
        if width <= maxWidth && height <= maxHeight {
            return image
        }
        
        let ratio = min(maxWidth / width, maxHeight / height)
        let newWidth = width * ratio
        let newHeight = height * ratio
        
        let size = CGSize(width: newWidth, height: newHeight)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { context in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
    }
}
