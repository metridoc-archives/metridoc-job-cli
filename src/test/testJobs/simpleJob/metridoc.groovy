import foo.bar.Foo
import grails.persistence.Entity
import metridoc.core.MetridocScript
import metridoc.tool.gorm.GormTool

println "foo ran"
new Foo()

use(MetridocScript) {
    includeTool(GormTool).enableGormFor(Bar)
}

Bar.list()

return "foo ran"

@Entity
class Bar {
    String foo
}