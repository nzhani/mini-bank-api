package kz.nurbol.minibank.common;

/** A rule of the domain was broken (e.g. insufficient funds). Mapped to HTTP 422. */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
