@Grab('org.apache.commons:commons-csv:1.2')
import org.apache.commons.csv.CSVParser
import static org.apache.commons.csv.CSVFormat.*

def mimicDirectory = new File(args[0])
def trainSize = args[1].toInteger()
def validSize = args[2].toInteger()
def testSize = args[3].toInteger()
def initialOffset = args[4].toInteger()
def eventCutoff = args[5].toInteger()

// borrowed from https://www.baeldung.com/java-add-hours-date
public Date addHoursToJavaUtilDate(Date date, int hours) {
	Calendar calendar = Calendar.getInstance();
	calendar.setTime(date);
	calendar.add(Calendar.HOUR_OF_DAY, hours);
	return calendar.getTime();
}

def rng = new Random(1337)
def sample = { rn, arr -> arr[rn.nextInt(arr.size())] }

def admissions = [:]
def admissionKeys = []
new File(mimicDirectory, 'ADMISSIONS.csv').splitEachLine(',') { fs ->
  if(fs[0] == '"ROW_ID"') { return; }

  // Throw away the entry if the patient died or was discharged before the given offset time ...
  def admissionDate = new Date().parse('yyyy-MM-dd hh:mm:ss', fs[3])
  def dischargeDate = new Date().parse('yyyy-MM-dd hh:mm:ss', fs[4])
  def deathDate
  if(fs[5] != '') {
    deathDate = new Date().parse('yyyy-MM-dd hh:mm:ss', fs[5])
  }
  def offsetAdmission = addHoursToJavaUtilDate(admissionDate, initialOffset)
  if(dischargeDate <= offsetAdmission || (deathDate && deathDate <= offsetAdmission)) {
    return;
  }

  def diedBeforeEventCutoff = false
  def offsetCutoff = addHoursToJavaUtilDate(offsetAdmission, eventCutoff)
  if(deathDate && deathDate < offsetCutoff) {
    diedBeforeEventCutoff = true
  }
  
  admissions[fs[2]] = [
    subject: fs[1],
    hadm: fs[2],
    admit: fs[3],
    disch: fs[4],
    dod: fs[5],
    eventCutoffDeath: diedBeforeEventCutoff
  ]
  admissionKeys << fs[2]
}

def hasTimedTextRecord = [:]
new File(mimicDirectory, 'NOTEEVENTS.csv').withReader { reader ->
  def csv = new CSVParser(reader, DEFAULT.withHeader())
  for(record in csv.iterator()) {
    if(admissions.containsKey(record['HADM_ID']) && record['CHARTTIME'] != '') {
      def admissionTime = new Date().parse('yyyy-MM-dd hh:mm:ss', admissions[record['HADM_ID']].admit)
      def offsetAdmission = addHoursToJavaUtilDate(admissionTime, initialOffset)
      def ct = new Date().parse('yyyy-MM-dd hh:mm:ss', record['CHARTTIME'])
      if(ct < offsetAdmission) {
        hasTimedTextRecord[record['HADM_ID']] = true
      }
    }
  }
}

println "Loaded ${admissions.size()} admissions"

def train = [:]
def trainDeadCount = 0
def trainLiveCount = 0

def valid = [:]
def test  = [:]

while(valid.size() < validSize) {
  def p = admissions[sample(rng, admissionKeys)]
  if(!train[p.hadm] && !valid[p.hadm] && hasTimedTextRecord.containsKey(p.hadm)) {
    valid[p.hadm] = p
  }
}

println "Sampled ${validSize} validation patients."

while(test.size() < testSize) {
  def p = admissions[sample(rng, admissionKeys)]
  if(!train.containsKey(p.hadm) && !valid.containsKey(p.hadm) && !test.containsKey(p.hadm) && hasTimedTextRecord.containsKey(p.hadm)) {
    test[p.hadm] = p
  }
}

println "Sampled ${testSize} validation patients."

while(train.size() < trainSize) {
  def p = admissions[sample(rng, admissionKeys)]
  if(!test.containsKey(p.hadm) && !valid.containsKey(p.hadm) && !train.containsKey(p.hadm) && hasTimedTextRecord.containsKey(p.hadm)) {
    if(p.eventCutoffDeath && trainLiveCount < (trainSize / 2)) { // something something odd number
      train[p.hadm] = p
      trainLiveCount++ 
    } else if(trainDeadCount < (trainSize / 2)) {
      train[p.hadm] = p
      trainDeadCount++ 
    }
  }
}

println "Sampled ${trainSize} training patients."



def headings = [ 'subject', 'hadm id', 'admission time', 'discharge time', 'death time', 'died before event cutoff', 'set' ]
def out = [ headings.join('\t') ] +
          train.collect { k, v -> v.collect { kk, kv -> kv }.join('\t') + '\ttrain' } + 
          valid.collect { k, v -> v.collect { kk, kv -> kv }.join('\t') + '\tvalid' } + 
          test.collect { k, v -> v.collect { kkv, kv -> kv }.join('\t') + '\ttest' }
new File('sampled_patients.tsv').text = out.join('\n')
