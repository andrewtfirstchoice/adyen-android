package uk.co.firstchoice_cs.core.helpers

import java.text.SimpleDateFormat
import java.util.*

object NextDayDelivery {

    const val cutoff = "19:00:00"
    const val hasFirebasePulled = false

    @JvmStatic
         fun cutoffComparator():String {
            //TIME MINUS 1 SECOND
         //   val calendar = Calendar.
                    //let dateFormatter = DateFormatter()
       //     dateFormatter.dateFormat = "HH:mm:ss"
         //   if let date = dateFormatter.date(from: cutoff) {
        //        var component = DateComponents()
        ////        component.second = -1
         //       if let actualDate = calendar.date(byAdding: component, to: date) {
        //        return dateFormatter.string(from: actualDate)
        //    }
       //     }
            return cutoff
        }

    val stringCutoff: String
        get() =
//            let dateFormatter = DateFormatter()
//            dateFormatter.dateFormat = "HH:mm:ss"
//            if let date = dateFormatter.date(from: cutoff) {
//                dateFormatter.dateFormat = "h:mma"
//                return dateFormatter.string(from: date)
//            }
//            return cutoff
            ""

    @JvmStatic
    fun holidays():Array<String> {
        return arrayOf(
            "25/12/2020",
            "28/12/2020",
            "01/01/2021",
            "02/04/2021",
            "05/04/2021",
            "03/05/2021",
            "31/05/2021",
            "30/08/2021",
            "27/12/2021",
            "28/12/2021",
            "03/01/2022",
            "15/04/2022",
            "18/04/2022",
            "02/05/2022",
            "03/05/2022",
            "29/08/2022",
            "26/12/2022",
            "27/12/2022",
            "02/01/2023",
            "07/04/2023",
            "10/04/2023",
            "01/05/2023",
            "29/05/2023",
            "28/08/2023",
            "25/12/2023",
            "26/12/2023",
            "01/01/2024",
            "29/03/2024",
            "01/04/2024",
            "06/05/2024",
            "27/05/2024",
            "26/08/2024",
            "25/12/2024",
            "26/12/2024",
            "01/01/2025",
            "18/04/2025",
            "21/04/2025",
            "05/05/2025",
            "26/05/2025",
            "25/08/2025",
            "25/12/2025",
            "26/12/2025"
        )
    }

    @JvmStatic
    fun adjustmentFor(day:Int):Int{
        return when(day) {
            5 -> 4
            6 -> 3
            7 -> 2
            else -> 1
        }
    }
    @JvmStatic
    fun weekdayAdjustment(weekday: Int):Int {
        return when(weekday) {
            6 -> 2
            7 -> 1
            else -> 0
        }
    }

    @JvmStatic
    fun getSimpleDateTime(date: Date): String {
        val df = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
        val dateString = df.format(date)
        val today = Date()
        val ret = dateString == df.format(today)
        if (ret) {
            val todayDf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            return "Today at " + todayDf.format(date)
        }
        return dateString
    }
    @JvmStatic
    fun numberOfBankHolidays(baseDate: Date, adjustment: Int):Int {
        var total = 0

     //   let calendar = Calendar.current
  //              let dateFormatter = DateFormatter()
     //   dateFormatter.dateFormat = "dd/MM/yyyy"
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

        for (i in 0..adjustment)
        {

        }
     //       println(args[i])

//        for i in 0...adjustment {
//            var dayComponent = DateComponents()
//            dayComponent.day = i
//            if let dateToCheck = calendar.date(byAdding: dayComponent, to: baseDate) {
//            let dateString = dateFormatter.string(from: dateToCheck)
//            if NextDayDelivery.holidays.contains(dateString) {
//                total += 1
//            }
//        }
//        }
        return total
    }



  /*  static func nextDeliveryDate() -> String {
        var adjustment = 0
        var baseDate: Date!

        let calendar = Calendar.current
        var dayComponent = DateComponents()
        let dateFormatter = DateFormatter()

        //CUTOFF DATE
        dateFormatter.dateFormat = "dd/MM/yyyy"

//        let overrideString = "24/12/2020"
//        let date = dateFormatter.date(from: overrideString)!
        let date = Date()

        let cutoffDateString = dateFormatter.string(from: date)+"T"+NextDayDelivery.cutoffComparator
        dateFormatter.dateFormat = "dd/MM/yyy'T'HH:mm:ss"
        if let cutoffDate = dateFormatter.date(from: cutoffDateString) {
            if date > cutoffDate {
                dayComponent.day = 1
                baseDate = calendar.date(byAdding: dayComponent, to: date)
            } else {
                baseDate = date
            }
        }
        //STANDARD DAY ADJUSTMENT
        let day = calendar.component(.day, from: date)
        adjustment += adjustmentFor(day: day)
        adjustment += numberOfBankHolidays(baseDate: baseDate, adjustment: adjustment)

        //FINAL DAY WEEKEND ADJUSTMENT
        dayComponent.day = adjustment
        if let date = calendar.date(byAdding: dayComponent, to: baseDate) {
            let weekdayToCheck = calendar.component(.weekday, from: date)
            adjustment += weekdayAdjustment(weekdayToCheck)
        }

        //DAY CONVERSION
        dateFormatter.dateFormat = "EEEE"
        dayComponent.day = adjustment
        if let finalDate = calendar.date(byAdding: dayComponent, to: baseDate) {
            //WEEKEND ADJUSTMENT
            let weekday = dateFormatter.string(from: finalDate).capitalized
            if adjustment == 1 {
                return "Tomorrow"
            } else {
                return weekday
            }
        }
        return ""
    }*/

  /*  static var attributedString: NSMutableAttributedString {
        let nextDayString = NextDayDelivery.nextDeliveryDate()
        let labelText = NSMutableAttributedString()

        let semiFont = UIFont(name: "Montserrat-SemiBold", size: 13)!
        let boldFont = UIFont(name: "Montserrat-Bold", size: 13.5)!

        var preString: NSMutableAttributedString!
        var dayString: NSMutableAttributedString!
        if nextDayString == "Tomorrow" {
            preString = NSMutableAttributedString(string: "Need your part ")
            dayString = NSMutableAttributedString(string: "\(nextDayString)")
        } else {
            preString = NSMutableAttributedString(string: "Need your part by ")
            dayString = NSMutableAttributedString(string: "\(nextDayString)")
        }
        dayString.addAttribute(NSAttributedString.Key.font, value: boldFont, range: NSRange(location: 0, length: dayString.length))

        let middleString = NSMutableAttributedString(string: "? Order by ")
        middleString.addAttribute(NSAttributedString.Key.font, value: semiFont, range: NSRange(location: 0, length: middleString.length))

        let cutoffString = NSMutableAttributedString(string: "\(NextDayDelivery.stringCutoff)")
        cutoffString.addAttribute(NSAttributedString.Key.font, value: boldFont, range: NSRange(location: 0, length: cutoffString.length))

        let finalString = NSMutableAttributedString(string: " and choose Next Day Delivery at Checkout")
        finalString.addAttribute(NSAttributedString.Key.font, value: semiFont, range: NSRange(location: 0, length: finalString.length))

        for string in [preString, dayString, middleString, cutoffString, finalString] {
            if let string = string { labelText.append(string) }
        }
        return labelText
    }
*/

}