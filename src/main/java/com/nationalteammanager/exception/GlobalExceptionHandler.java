package com.nationalteammanager.exception;

import com.nationalteammanager.config.RefererPaths;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException exception) {
        return errorPage(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleMissingPage() {
        return errorPage(HttpStatus.NOT_FOUND, "error.page.notFound");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleForbidden() {
        return errorPage(HttpStatus.FORBIDDEN, "error.adminOnly").addObject("adminOnly", true);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleInvalidArgument() {
        return errorPage(HttpStatus.BAD_REQUEST, "error.badArgument");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handleUnsupportedMethod() {
        return errorPage(HttpStatus.METHOD_NOT_ALLOWED, "error.methodNotAllowed");
    }

    /** Poslovna greška izvan obrasca (npr. brisanje): korisnik ostaje na istoj stranici i vidi poruku. */
    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRule(BusinessRuleException exception, HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        return redirectBack(text(exception.getCode(), exception.getArguments()), request, redirectAttributes);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        log.warn("Narušeno ograničenje baze: {}", exception.getMostSpecificCause().getMessage());
        return redirectBack(text("error.integrity"), request, redirectAttributes);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleUploadTooLarge(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        return redirectBack(text("error.upload.tooLarge"), request, redirectAttributes);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception exception) {
        log.error("Neočekivana greška", exception);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "error.unexpected");
    }

    private ModelAndView errorPage(HttpStatus status, String messageCode) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("message", text(messageCode));
        return modelAndView;
    }

    private String redirectBack(String message, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:" + RefererPaths.pathOf(request);
    }

    private String text(String messageCode, Object... arguments) {
        return messageSource.getMessage(messageCode, arguments, LocaleContextHolder.getLocale());
    }
}
