import static metridoc.iterators.Iterators.*

createIterator(file: new File("people.csv"), "csv").each {
    println  it
}