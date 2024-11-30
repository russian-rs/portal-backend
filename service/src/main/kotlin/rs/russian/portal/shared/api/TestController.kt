package rs.russian.portal.shared.api

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.util.ClassUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@Profile("local")
@RestController
class TestController(
    private val applicationContext: ApplicationContext
) {

    @OptIn(DelicateCoroutinesApi::class)
    @GetMapping("/test/{bean}/{method}")
    fun test(@PathVariable("bean") beanName: String, @PathVariable("method") methodName: String): String? {
        try {
            val bean = applicationContext.getBean(beanName)
            val userClass = ClassUtils.getUserClass(bean)
            val method = userClass.getMethod(methodName)

            GlobalScope.launch {
                method.invoke(bean)
            }
            return "OK"
        } catch (e: Exception) {
            return e.message
        }
    }
}
