import ExpoModulesCore
import PassKit

public class ExpoInAppProvisioningModule: Module {
  var addPaymentPassRequestCompletionHandler: ((PKAddPaymentPassRequest) -> Void)?

  public func definition() -> ModuleDefinition {
      Name("ExpoInAppProvisioning")

      AsyncFunction("isAvailable") { () -> Bool in
          return canAddPaymentPass()
      }
      
      AsyncFunction("canAddCard") { (cardId: String) -> Bool in
        if !canAddPaymentPass() {
            return false
        }
        return canAddPaymentPass(withPrimaryAccountIdentifier: cardId)
      }
      
      AsyncFunction("presentAddPaymentPassViewController") { (
        cardholderName: String,
        localizedDescription: String,
        primaryAccountSuffix: String,
        primaryAccountIdentifier: String,
        promise: Promise
      ) throws -> Promise in
          presentAddPaymentPassViewController(
              cardholderName: cardholderName,
              localizedDescription: localizedDescription,
              primaryAccountSuffix: primaryAccountSuffix,
              primaryAccountIdentifier: primaryAccountIdentifier,
              promise: promise
          )
          return promise
      }
      
      AsyncFunction("pushProvision") { (
        activationDataString: String,
        encryptedPassDataString: String,
        ephemeralPublicKeyString: String,
        promise: Promise
      ) throws -> Promise in
          callAddPaymentPassRequestHandler(
                activationDataString: activationDataString,
                encryptedPassDataString: encryptedPassDataString,
                ephemeralPublicKeyString: ephemeralPublicKeyString,
                promise: promise
          )
          return promise
      }
    }
    
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
    
    func presentAddPaymentPassViewController(
        cardholderName: String,
        localizedDescription: String,
        primaryAccountSuffix: String,
        primaryAccountIdentifier: String,
        promise: Promise
    ) {
        let configuration = PKAddPaymentPassRequestConfiguration(encryptionScheme: .ECC_V2)
        
        configuration!.cardholderName = cardholderName
        configuration!.localizedDescription = localizedDescription
        configuration!.paymentNetwork = .visa
        configuration!.primaryAccountSuffix = primaryAccountSuffix
        configuration!.primaryAccountIdentifier = primaryAccountIdentifier
        let delegate = AddPaymentPassViewControllerDelegate(promise: promise, completionHandler: addPaymentPassRequestCompletionHandler)
        let passView = PKAddPaymentPassViewController(requestConfiguration: configuration!, delegate: delegate)
        if passView != nil {
            DispatchQueue.main.async {
                // Find the appropriate window to present the view controller
                    if let window = UIApplication.shared.windows.first(where: { $0.isKeyWindow }) {
                        window.rootViewController?.present(passView!, animated: true)
//                        {
//                            self.sendEvent(withName: "addToWalletViewShown", body: ["args": args])
//                            promise.resolve(nil)
//                            return
//                        }
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
        if let activationData = Data(base64Encoded: activationDataString),
           let encryptedPassData = Data(base64Encoded: encryptedPassDataString),
           let ephemeralPublicKey = Data(base64Encoded: ephemeralPublicKeyString) {
            
            let paymentPassRequest = PKAddPaymentPassRequest()
            paymentPassRequest.activationData = activationData
            paymentPassRequest.encryptedPassData = encryptedPassData
            paymentPassRequest.ephemeralPublicKey = ephemeralPublicKey
            
            if let completionHandler = self.addPaymentPassRequestCompletionHandler {
                completionHandler(paymentPassRequest)
                promise.resolve(paymentPassRequest)
            } else {
                NSLog("Error : Completion handler was not set")
                promise.reject("Completion handler was not set", "AddPaymentPassRequestCompletionHandler was not set")
            }
        } else {
            promise.reject("Invalid base64 data", "Failed to decode base64 data")
        }
    }

}

class AddPaymentPassViewControllerDelegate: NSObject, PKAddPaymentPassViewControllerDelegate {
    let promise: Promise
    var completionHandler: ((PKAddPaymentPassRequest) -> Void)?
    
    init(promise: Promise, completionHandler: ((PKAddPaymentPassRequest) -> Void)?) {
        self.promise = promise
        self.completionHandler = completionHandler
    }
    
    func addPaymentPassViewController(_ controller: PKAddPaymentPassViewController, generateRequestWithCertificateChain certificates: [Data], nonce: Data, nonceSignature: Data, completionHandler handler: @escaping (PKAddPaymentPassRequest) -> Void) {
        NSLog("addPaymentPassViewController delegate to generate cert chain, nonce, and nonce signature")
        
        self.completionHandler = handler
        
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
//            self.sendEvent(withName: "getPaymentPassInfo", body: ["args": args])
            promise.resolve(args)
        } else {
            promise.reject("Error getting data", "Could not initialize process")
        }
    }
    
    func addPaymentPassViewController(_ controller: PKAddPaymentPassViewController, didFinishAdding pass: PKPaymentPass?, error: Error?) {
        NSLog("pass: \(String(describing: pass)) | error: \(String(describing: error))")
        
        PKAddPaymentPassViewController().dismiss(animated: true)
        controller.dismiss(animated: true) {
            if let pass = pass {
                self.promise.resolve(pass)
            } else {
                self.promise.reject("addingPassFailed", "Failed to add card")
            }
        }
    }
}
