package rs.russian.portal.shared.api

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.util.ClassUtils
import org.springframework.web.bind.annotation.*
import rs.russian.portal.mail.service.EmailService
import rs.russian.portal.shared.enums.UserGroup.ADMIN
import rs.russian.portal.shared.security.Authorized

@RestController
class TestController(
    private val emailService: EmailService,
    private val applicationContext: ApplicationContext
) {

    @Authorized(allowed = [ADMIN])
    @OptIn(DelicateCoroutinesApi::class)
    @GetMapping("/scheduler/{bean}/{method}")
    fun scheduler(@PathVariable("bean") beanName: String, @PathVariable("method") methodName: String): String? {
        try {
            val bean = applicationContext.getBean(beanName)
            val userClass = ClassUtils.getUserClass(bean)
            val method = userClass.getMethod(methodName)

            if (method.getAnnotation(Scheduled::class.java) == null) {
                return "Not a scheduler"
            }

            GlobalScope.launch {
                method.invoke(bean)
            }
            return "OK"
        } catch (e: Exception) {
            return e.message
        }
    }

    @Authorized(allowed = [ADMIN])
    @PostMapping("/email/test")
    fun sendTestEmail(
        @RequestParam(name = "to", required = true) to: String,
        @RequestBody(required = true) text: String
    ): String? {
        emailService.sendCommonEmail(to, subject = "Тестовое письмо", text = text)
        return "OK"
    }
}
