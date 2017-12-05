// conversion of text format with one sentence per line to individual 
// gate documents with one document per k sentences.  
// First parameter is the output directory
// Second parameter is a file name prefix to use for the output document
// Third parameter is the value to assign to the "class" feature of the "Sentence"
// annotation in the Key set.

import gate.*
import java.utils.*
import groovy.util.CliBuilder

def cli = new CliBuilder(usage:'sentences2gate.groovy [-h] [-n 1] outdir fileprefix classlabel < infile')
cli.h(longOpt: 'help', "Show usage information")
cli.n(longOpt: 'nsent', args: 1, argName: 'nsent', "Number of sentences per output document (0=single output document, default: 1)")
cli.v(longOpt: 'verbose', "Show more verbose information")
cli.d(longOpt: 'debug', "Show debugging information")

def options = cli.parse(args)
if(options.h) {
  cli.usage()
  return
}

debug = options.d
verbose = options.d || options.v

def nsent = 1
if(options.n) {
  nsent = options.n.toInteger()
}

def posArgs = options.arguments()
if(posArgs.size() != 3) {
  cli.usage()
  System.exit(1)
}

outDir = new File(posArgs[0])
outFilePrefix = posArgs[1]
classLabel = posArgs[2]

if(!outDir.exists() || !outDir.isDirectory()) {
  System.err.println("ERROR: file does not exist or is not a directory: "+outDir.getAbsolutePath())
  System.exit(1)
}

System.err.println("INFO: output dir is:        "+outDir)
System.err.println("INFO: sentences per doc:    "+nsent)

gate.Gate.init()

gate.Gate.getUserConfig().put(gate.GateConstants.DOCEDIT_INSERT_PREPEND,true)

wordList = []

nSent = 0       // current sentence number, starting counting with 1
nLine = 0       // current line number, countring from 1
nDoc = 0        // current document number, counting from 1
nErrors = 0

// holds the current document where a sentence should get added to, or is
// null if we do not have a document yet, or if we just wrote a document.
// So, whenever this is not-null, we have something that needs eventually get
// written out.
curDoc = null


br = new BufferedReader(new InputStreamReader(System.in,"UTF-8"))
while((line = br.readLine())!= null){
  nLine += 1
  line = line.trim()
  nSent += 1
  curDoc = addSentenceToDocument(curDoc, line, nSent, nLine)
  curDoc = writeDocumentIfNeeded(curDoc, outDir, nsent, nLine, false)
}
// Write out any partially created document, if there is one. This does nothing
// if curDoc is null.
if(nsent==0)
  writeDocumentIfNeeded(curDoc, outDir, 0, nLine, true)
else
  writeDocumentIfNeeded(curDoc, outDir, 1, nLine, true)


System.err.println("INFO: number of lines read:        "+nLine)
System.err.println("INFO: number of sentences found:   "+nSent)
System.err.println("INFO: number of documents written: "+nDoc)
System.err.println("INFO: number of errors:            "+nErrors)

def addSentenceToDocument(doc, text, nSent, nLineTo) {
  // if the doc is null, create a new one which will later returned, otherwise
  // the one we got will get returned
  if(doc == null) {
    parms = Factory.newFeatureMap()
    parms.put(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME, "")
    parms.put(Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, "text/plain")
    doc = (Document) gate.Factory.createResource("gate.corpora.DocumentImpl", parms)
    doc.getFeatures().put("nSentFrom",nSent)
  }
  doc.getFeatures().put("nSentTo",nSent)
  outputAS = doc.getAnnotations("Key")
  curOffsetFrom = doc.getContent().size()
  // if We already have something (another sentence), then first add 
  // a new line to the document to separate the sentence we are going to add  
  if(curOffsetFrom > 0) {
    doc.edit(curOffsetFrom,curOffsetFrom,new gate.corpora.DocumentContentImpl("\n"))
    curOffsetFrom = doc.getContent().size()
  }
  startOffset = doc.getContent().size()
  doc.edit(startOffset,startOffset,new gate.corpora.DocumentContentImpl(text))
  endOffset = doc.getContent().size()
  fm = gate.Factory.newFeatureMap()
  fm.put("class",classLabel)
  fm.put("gate.conversion.nSent",nSent)
  fm.put("gate.conversion.nLineTo",nLineTo)
  gate.Utils.addAnn(outputAS,startOffset,endOffset,"Sentence",fm)
  
  return doc
}

// write out the document if needed and either return the original document or a new one
// if nsent > 0 then we write if the current number of sentences already in the document
// has reached nsent. 
// if nsent is <=0, we only output the document if force is true.
def writeDocumentIfNeeded(doc, outDir, nsent,nLine,force) {
  if(doc==null) {
    return doc
  }
  sFrom = (int)doc.getFeatures().get("nSentFrom")
  sTo = (int)doc.getFeatures().get("nSentTo")
  haveSents = sTo-sFrom+1
  // if nsent is 0 (indicating we should only output at the end of processing, when force=true)
  // then only output if force is true as well
  if((nsent==0 && force) || (nsent > 0 && haveSents >= nsent)) {
    if(nsent==0) {
      name = outFilePrefix + ".gate.xml"      
    } else if(haveSents == 1) {
      name = outFilePrefix + ".gate.s"+sFrom+".xml"
    } else {
      name = outFilePrefix + ".gate.s"+sFrom+"_"+sTo+".xml"
    }
    outFile = new File(outDir,name)
    gate.corpora.DocumentStaxUtils.writeDocument(doc,outFile)  
    if(verbose) System.err.println("Document saved: "+outFile)
    nDoc += 1
    gate.Factory.deleteResource(doc)
    doc = null
  }
  return doc
}
