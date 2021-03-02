@Grab('org.apache.commons:commons-csv:1.2')
import org.apache.commons.csv.CSVParser
import static org.apache.commons.csv.CSVFormat.*

def mimicDirectory = new File(args[0])
def trainSize = args[1].toInteger()
def validSize = args[2].toInteger()
def testSize = args[3].toInteger()

def rng = new Random(1337)
def sample = { rn, arr -> arr[rn.nextInt(arr.size())] }

def hasTimedTextRecord = [:]
new File(mimicDirectory, 'NOTEEVENTS.csv').withReader { reader ->
  def csv = new CSVParser(reader, DEFAULT.withHeader())
  for(record in csv.iterator()) {
    if(record['CHARTTIME'] != '') {
      hasTimedTextRecord[record['HADM_ID']] = true
    }
  }
}

def admissions = []
new File(mimicDirectory, 'ADMISSIONS.csv').splitEachLine(',') { fs ->
  if(fs[0] == '"ROW_ID"') { return; }
  admissions << [
    subject: fs[1],
    hadm: fs[2],
    dod: fs[5]
  ]
}

println "Loaded ${admissions.size()} admissions"

def train = [:]
def trainDeadCount = 0
def trainLiveCount = 0

def valid = [:]
def test  = [:]

while(train.size() < trainSize) {
  def p = sample(rng, admissions)
  if(!train.containsKey(p.hadm) && hasTimedTextRecord.containsKey(p.hadm)) {
    if(p.dod == '' && trainLiveCount < (trainSize / 2)) { // something something odd number
      train[p.hadm] = p
      trainLiveCount++ 
      println 'train count: ' + trainLiveCount
    } else if(trainDeadCount < (trainSize / 2)) {
      train[p.hadm] = p
      trainDeadCount++ 
      println 'train dead count: ' + trainDeadCount
    }
  }
}

println "Sampled ${trainSize} training patients."
println trainDeadCount

while(valid.size() < validSize) {
  def p = sample(rng, admissions)
  if(!train[p.hadm] && !valid[p.hadm] && hasTimedTextRecord.containsKey(p.hadm)) {
    valid[p.hadm] = p
  }
}

println "Sampled ${validSize} validation patients."

while(test.size() < testSize) {
  def p = sample(rng, admissions)
  if(!train[p.hadm] && !valid[p.hadm] && !test[p.hadm] && hasTimedTextRecord.containsKey(p.hadm)) {
    test[p.hadm] = p
  }
}

println "Sampled ${testSize} validation patients."

def out = train.collect { k, v -> v.collect { kk, kv -> kv }.join('\t') + '\ttrain' } + 
          valid.collect { k, v -> v.collect { kk, kv -> kv }.join('\t') + '\tvalid' } + 
          test.collect { k, v -> v.collect { kkv, kv -> kv }.join('\t') + '\ttest' }
new File('sampled_patients.tsv').text = out.join('\n')
