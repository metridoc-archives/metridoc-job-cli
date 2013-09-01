@Grab(group='com.github.stefanbirkner', module='system-rules', version='1.3.1')
import org.junit.contrib.java.lang.system.StandardOutputStreamLog
import org.springframework.core.io.ClassPathResource

assert new ClassPathResource("fileInFoo.txt").exists()

println "complex foo ran"
return "complex foo project ran"
