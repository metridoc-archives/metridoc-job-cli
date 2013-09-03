import entity.Bar
import metridoc.core.MetridocScript
import metridoc.core.tools.HibernateTool
@Grab(group='com.github.stefanbirkner', module='system-rules', version='1.3.1')
import org.junit.contrib.java.lang.system.StandardOutputStreamLog
import org.springframework.core.io.ClassPathResource

assert new ClassPathResource("fileInFoo.txt").exists()

//testing to see classpath works with hibernate properly
use(MetridocScript) {
    includeTool(entityClasses: [Bar], embeddedDataSource:true, HibernateTool)

}

println "complex foo ran"
return "complex foo project ran"
