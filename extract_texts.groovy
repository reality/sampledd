@Grab('org.apache.commons:commons-csv:1.2')
import org.apache.commons.csv.CSVParser
import static org.apache.commons.csv.CSVFormat.*

// borrowed from https://www.baeldung.com/java-add-hours-date
public Date addHoursToJavaUtilDate(Date date, int hours) {
	Calendar calendar = Calendar.getInstance();
	calendar.setTime(date);
	calendar.add(Calendar.HOUR_OF_DAY, hours);
	return calendar.getTime();
}

def mimicDirectory = new File(args[0])
def hoursOffset = args[1].toInteger()
def spts = [:]
def offsets = [:]
new File('sampled_patients.tsv').splitEachLine('\t') {
  if(it[0] == 'subject') { return; }
  spts[it[1]] = it
  def admissionDate = new Date().parse('yyyy-MM-dd hh:mm:ss', it[2])
  offsets[it[1]] = addHoursToJavaUtilDate(admissionDate, hoursOffset)
}

def earliestRecords = [:]
new File(mimicDirectory, 'NOTEEVENTS.csv').withReader { reader ->
  CSVParser csv = new CSVParser(reader, DEFAULT.withHeader())

  for(record in csv.iterator()) {
    if(spts.containsKey(record['HADM_ID']) && record['CHARTTIME'] != '') {
      def time = new Date().parse('yyyy-MM-dd hh:mm:ss', record['CHARTTIME'])
      if(time < offsets[record['HADM_ID']]) {
        if(!earliestRecords.containsKey(record['HADM_ID'])) {
          earliestRecords[record['HADM_ID']] = []
        }
        earliestRecords[record['HADM_ID']] << record
        println 'Added new record to ' + record['HADM_ID']
      }
    }
  }
}

def outDir = new File('texts')
if(!outDir.exists()) { outDir.mkdir() }

earliestRecords.each { k, v ->
  new File(outDir, k + '.txt').text = v.collect { it['TEXT'].replaceAll('\n', '').replaceAll('\\s+', ' ').replaceAll('\\.', '. ') }.join('\n')
}



