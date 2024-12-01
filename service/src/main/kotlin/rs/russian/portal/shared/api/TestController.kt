package rs.russian.portal.shared.api

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.util.ClassUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import rs.russian.portal.shared.enums.UserGroup.ADMIN
import rs.russian.portal.shared.security.Authorized

@RestController
class TestController(
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
}
