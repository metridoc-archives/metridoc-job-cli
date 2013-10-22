import foo.bar.Foo
import grails.persistence.Entity
import metridoc.tool.gorm.GormTool
import metridoc.core.services.ParseArgsService


new Foo()

includeService(ParseArgsService)
includeService(GormTool).enableFor(Bar)


Bar.list()

println "foo ran"
return "foo ran"

@Entity
class Bar {
    String foo
}