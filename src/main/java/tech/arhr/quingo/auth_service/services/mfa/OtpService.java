package tech.arhr.quingo.auth_service.services.mfa;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.utils.EncryptionUtil;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final EncryptionUtil encryptionUtil;

    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    public String createUriForSecret(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .issuer("Quingo")
                .secret(secret)
                .build();

        return data.getUri();
    }

    public boolean verifyCode(String encryptedSecret, String code) {
        String secret = encryptionUtil.decrypt(encryptedSecret);

        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());

        verifier.setTimePeriod(30);
        verifier.setAllowedTimePeriodDiscrepancy(1);

        return verifier.isValidCode(secret, code);
    }
}
