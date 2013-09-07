import metridoc.core.MetridocScript
import metridoc.core.tools.HibernateTool

/*
    This test assumes that mdoc is run with -localMysql and -mergeMetridocConfig=false command
    see HibernateDataSourceSpec
 */
use(MetridocScript) {
    def tool = includeTool(HibernateTool)
    println config
    assert tool.localMysql : "localMysql should be true"
    assert !tool.mergeMetridocConfig : "we should not be merging the metridoc config"
    assert config.dataSource.url == "jdbc:mysql://localhost:3306/test"
}


