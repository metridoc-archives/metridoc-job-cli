import org.springframework.core.io.ClassPathResource

assert new ClassPathResource("fileInFoo.txt").exists()

println "complex foo ran"
return "complex foo project ran"
