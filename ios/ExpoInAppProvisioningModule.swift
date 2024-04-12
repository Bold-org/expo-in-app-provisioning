import ExpoModulesCore
import PassKit

public class ExpoInAppProvisioningModule: Module {
    var paymentPass = PaymentPass()
    
    public func definition() -> ModuleDefinition {
        Name("ExpoInAppProvisioning")
        
        AsyncFunction("openWallet") { () -> Bool in
            return paymentPass.openWallet()
        }
        
        AsyncFunction("isAvailable") { () -> Bool in
            return paymentPass.canAddPaymentPass()
        }
        
        AsyncFunction("canAddCard") { (cardId: String) -> Bool in
            if !paymentPass.canAddPaymentPass() {
                return false
            }
            return paymentPass.canAddPaymentPass(withPrimaryAccountIdentifier: cardId)
        }
        
        AsyncFunction("presentAddPaymentPassViewController") { (
            cardholderName: String,
            localizedDescription: String,
            primaryAccountSuffix: String,
            primaryAccountIdentifier: String,
            promise: Promise
        ) throws -> Promise in
            paymentPass.presentAddPaymentPassViewController(
                cardholderName: cardholderName,
                localizedDescription: localizedDescription,
                primaryAccountSuffix: primaryAccountSuffix,
                primaryAccountIdentifier: primaryAccountIdentifier,
                promise: promise
            )
            return promise
        }

        AsyncFunction("dismissAddPaymentPassViewController") { (promise: Promise) -> Void in
            return paymentPass.dismissAddPaymentPassViewController(promise: promise)
        }
        
        AsyncFunction("pushProvision") { (
            activationDataString: String,
            encryptedPassDataString: String,
            ephemeralPublicKeyString: String,
            promise: Promise
        ) throws -> Promise in
            paymentPass.callAddPaymentPassRequestHandler(
                activationDataString: activationDataString,
                encryptedPassDataString: encryptedPassDataString,
                ephemeralPublicKeyString: ephemeralPublicKeyString,
                promise: promise
            )
            return promise
        }
    }
}


class PaymentPass: NSObject {
    var addPaymentPassRequestCompletionHandler: ((PKAddPaymentPassRequest) -> Void)?
    var successCallback: EXPromiseResolveBlock?
    var errorCallback: EXPromiseRejectBlock?
    var hasBeenInitialized = false
    
    func canAddPaymentPass() -> Bool {
        if #available(iOS 9.0, *) {
            return PKAddPaymentPassViewController.canAddPaymentPass()
        } else {
            return false
        }
    }
    
    func canAddPaymentPass(withPrimaryAccountIdentifier cardId: String) -> Bool {
        let library = PKPassLibrary()
        if #available(iOS 13.4, *) {
            return library.canAddSecureElementPass(primaryAccountIdentifier: cardId)
        }
        if #available(iOS 9.0, *) {
            return library.canAddPaymentPass(withPrimaryAccountIdentifier: cardId)
        } else {
            return false
        }
    }
    
    func openWallet() -> Bool {
        let library = PKPassLibrary()
        if #available(iOS 8.3, *) {
            library.openPaymentSetup()
            return true
        } else {
            return false
        }
    }
    
    func presentAddPaymentPassViewController(
        cardholderName: String,
        localizedDescription: String,
        primaryAccountSuffix: String,
        primaryAccountIdentifier: String,
        promise: Promise
    ) {
        successCallback = promise.resolve
        errorCallback = promise.legacyRejecter
        
        let configuration = PKAddPaymentPassRequestConfiguration(encryptionScheme: .ECC_V2)
        configuration!.cardholderName = cardholderName
        configuration!.localizedDescription = localizedDescription
        configuration!.paymentNetwork = .visa
        configuration!.primaryAccountSuffix = primaryAccountSuffix
        configuration!.primaryAccountIdentifier = primaryAccountIdentifier
        
        let passView = PKAddPaymentPassViewController(
            requestConfiguration: configuration!,
            delegate: self
        )
        
        if passView != nil {
            DispatchQueue.main.async {
                // Find the appropriate window to present the view controller
                if let window = UIApplication.shared.windows.first(where: { $0.isKeyWindow }) {
                    window.rootViewController?.present(passView!, animated: true)
                }
            }
        } else {
            promise.reject("Payment pass view creation error", "Unable to create the view")
        }
    }
    
    func callAddPaymentPassRequestHandler(
        activationDataString: String, 
        encryptedPassDataString: String,
        ephemeralPublicKeyString: String,
        promise: Promise
    ) {
        successCallback = promise.resolve
        errorCallback = promise.legacyRejecter
        
        if let activationData = Data(base64Encoded: activationDataString),
           let encryptedPassData = Data(base64Encoded: encryptedPassDataString),
           let ephemeralPublicKey = Data(base64Encoded: ephemeralPublicKeyString) {
            
            let paymentPassRequest = PKAddPaymentPassRequest()
            paymentPassRequest.activationData = activationData
            paymentPassRequest.encryptedPassData = encryptedPassData
            paymentPassRequest.ephemeralPublicKey = ephemeralPublicKey
            
            if let completionHandler = self.addPaymentPassRequestCompletionHandler {
                completionHandler(paymentPassRequest)
                promise.resolve(true)
            } else {
                NSLog("Error : Completion handler was not set")
                promise.reject("Completion handler was not set", "AddPaymentPassRequestCompletionHandler was not set")
            }
        } else {
            promise.reject("Invalid base64 data", "Failed to decode base64 data")
        }
    }
    
    func dismissAddPaymentPassViewController(promise: Promise) {
        DispatchQueue.main.async {
            guard let window = UIApplication.shared.windows.first(where: { $0.isKeyWindow }) else {
                promise.reject("AddPaymentPassViewController dismissal error", "AddPaymentPassViewController was not dismissed")
                return
            }
            
            guard let rootViewController = window.rootViewController else {
                promise.reject("AddPaymentPassViewController dismissal error", "AddPaymentPassViewController was not dismissed")
                return
            }
            
            guard let presentedViewController = rootViewController.presentedViewController else {
                promise.reject("AddPaymentPassViewController dismissal error", "AddPaymentPassViewController was not dismissed")
                return
            }
            
            rootViewController.dismiss(animated: true) {
                self.hasBeenInitialized = false
                promise.resolve()
            }
        }
    }

}

extension PaymentPass: PKAddPaymentPassViewControllerDelegate {
    func addPaymentPassViewController(_ controller: PKAddPaymentPassViewController, generateRequestWithCertificateChain certificates: [Data], nonce: Data, nonceSignature: Data, completionHandler handler: @escaping (PKAddPaymentPassRequest) -> Void) {
        NSLog("addPaymentPassViewController delegate to generate cert chain, nonce, and nonce signature")
        
        self.hasBeenInitialized = true
        self.addPaymentPassRequestCompletionHandler = handler
        
        // The leaf certificate will be the first element of that array and the sub-CA certificate will follow.
        let leafCertData = certificates.first?.base64EncodedString()
        let subCACertData = certificates.dropFirst().first?.base64EncodedString()
        let nonceData = nonce.base64EncodedString()
        let nonceSigData = nonceSignature.base64EncodedString()
        
        if ((leafCertData != nil) && (subCACertData != nil)) {
            let args: [String: Any] = [
                "leafCertificate": leafCertData!,
                "subCACertificate": subCACertData!,
                "nonce": nonceData,
                "nonceSignature": nonceSigData
            ]
            
            NSLog("Event send to JS with certs, nonce, and nonceSignature")
            self.successCallback?(args)
        } else {
            self.hasBeenInitialized = false
            self.errorCallback?("Error getting data", "Could not initialize process", nil)
        }
    }
    
    func addPaymentPassViewController(_ controller: PKAddPaymentPassViewController, didFinishAdding pass: PKPaymentPass?, error: Error?) {
        NSLog("pass: \(String(describing: pass)) | error: \(String(describing: error))")
        
        PKAddPaymentPassViewController().dismiss(animated: true)
        controller.dismiss(animated: true) {
            if let pass = pass {
                self.successCallback?(pass)
                return
            }
            if self.hasBeenInitialized, let error = error {
                self.errorCallback?("addingPassFailed", "Failed to add card", error)
                return
            }
            // cancelled
            self.successCallback?(nil)
            self.hasBeenInitialized = false
        }
    }
}
