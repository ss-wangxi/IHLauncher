package cc.snser.launcher.widget.calendar1x1;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.btime.launcher.R;

import android.content.Context;

public class FullCalendar {

    private Context context;

    private static final int CALENDAR_TOP_YEAR = 2100;

    private static final int CALENDAR_BOTTOM_YEAR = 1901;

    private static final int DEFAULT_VALUE = -1;

    private static final char[] DAYS_IN_GREGORIAN_MONTH = {
        31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    /** 农历节日 */
    private static Holiday[] mChineseHolidayTab;

    /**
     * 农历月份大小压缩表，两个字节表示一年。两个字节共十六个二进制位数， 前四个位数表示闰月月份，后十二个位数表示十二个农历月份的大小。
     */
    private static final char[] CHINESE_MONTH_INFO = {
        0x00, 0x04, 0xad, 0x08, 0x5a, 0x01, 0xd5, 0x54, 0xb4, 0x09, 0x64, 0x05, 0x59, 0x45, 0x95, 0x0a, 0xa6, 0x04, 0x55, 0x24, 0xad, 0x08, 0x5a, 0x62, 0xda, 0x04, 0xb4, 0x05, 0xb4, 0x55, 0x52, 0x0d,
        0x94, 0x0a, 0x4a, 0x2a, 0x56, 0x02, 0x6d, 0x71, 0x6d, 0x01, 0xda, 0x02, 0xd2, 0x52, 0xa9, 0x05, 0x49, 0x0d, 0x2a, 0x45, 0x2b, 0x09, 0x56, 0x01, 0xb5, 0x20, 0x6d, 0x01, 0x59, 0x69, 0xd4, 0x0a,
        0xa8, 0x05, 0xa9, 0x56, 0xa5, 0x04, 0x2b, 0x09, 0x9e, 0x38, 0xb6, 0x08, 0xec, 0x74, 0x6c, 0x05, 0xd4, 0x0a, 0xe4, 0x6a, 0x52, 0x05, 0x95, 0x0a, 0x5a, 0x42, 0x5b, 0x04, 0xb6, 0x04, 0xb4, 0x22,
        0x6a, 0x05, 0x52, 0x75, 0xc9, 0x0a, 0x52, 0x05, 0x35, 0x55, 0x4d, 0x0a, 0x5a, 0x02, 0x5d, 0x31, 0xb5, 0x02, 0x6a, 0x8a, 0x68, 0x05, 0xa9, 0x0a, 0x8a, 0x6a, 0x2a, 0x05, 0x2d, 0x09, 0xaa, 0x48,
        0x5a, 0x01, 0xb5, 0x09, 0xb0, 0x39, 0x64, 0x05, 0x25, 0x75, 0x95, 0x0a, 0x96, 0x04, 0x4d, 0x54, 0xad, 0x04, 0xda, 0x04, 0xd4, 0x44, 0xb4, 0x05, 0x54, 0x85, 0x52, 0x0d, 0x92, 0x0a, 0x56, 0x6a,
        0x56, 0x02, 0x6d, 0x02, 0x6a, 0x41, 0xda, 0x02, 0xb2, 0xa1, 0xa9, 0x05, 0x49, 0x0d, 0x0a, 0x6d, 0x2a, 0x09, 0x56, 0x01, 0xad, 0x50, 0x6d, 0x01, 0xd9, 0x02, 0xd1, 0x3a, 0xa8, 0x05, 0x29, 0x85,
        0xa5, 0x0c, 0x2a, 0x09, 0x96, 0x54, 0xb6, 0x08, 0x6c, 0x09, 0x64, 0x45, 0xd4, 0x0a, 0xa4, 0x05, 0x51, 0x25, 0x95, 0x0a, 0x2a, 0x72, 0x5b, 0x04, 0xb6, 0x04, 0xac, 0x52, 0x6a, 0x05, 0xd2, 0x0a,
        0xa2, 0x4a, 0x4a, 0x05, 0x55, 0x94, 0x2d, 0x0a, 0x5a, 0x02, 0x75, 0x61, 0xb5, 0x02, 0x6a, 0x03, 0x61, 0x45, 0xa9, 0x0a, 0x4a, 0x05, 0x25, 0x25, 0x2d, 0x09, 0x9a, 0x68, 0xda, 0x08, 0xb4, 0x09,
        0xa8, 0x59, 0x54, 0x03, 0xa5, 0x0a, 0x91, 0x3a, 0x96, 0x04, 0xad, 0xb0, 0xad, 0x04, 0xda, 0x04, 0xf4, 0x62, 0xb4, 0x05, 0x54, 0x0b, 0x44, 0x5d, 0x52, 0x0a, 0x95, 0x04, 0x55, 0x22, 0x6d, 0x02,
        0x5a, 0x71, 0xda, 0x02, 0xaa, 0x05, 0xb2, 0x55, 0x49, 0x0b, 0x4a, 0x0a, 0x2d, 0x39, 0x36, 0x01, 0x6d, 0x80, 0x6d, 0x01, 0xd9, 0x02, 0xe9, 0x6a, 0xa8, 0x05, 0x29, 0x0b, 0x9a, 0x4c, 0xaa, 0x08,
        0xb6, 0x08, 0xb4, 0x38, 0x6c, 0x09, 0x54, 0x75, 0xd4, 0x0a, 0xa4, 0x05, 0x45, 0x55, 0x95, 0x0a, 0x9a, 0x04, 0x55, 0x44, 0xb5, 0x04, 0x6a, 0x82, 0x6a, 0x05, 0xd2, 0x0a, 0x92, 0x6a, 0x4a, 0x05,
        0x55, 0x0a, 0x2a, 0x4a, 0x5a, 0x02, 0xb5, 0x02, 0xb2, 0x31, 0x69, 0x03, 0x31, 0x73, 0xa9, 0x0a, 0x4a, 0x05, 0x2d, 0x55, 0x2d, 0x09, 0x5a, 0x01, 0xd5, 0x48, 0xb4, 0x09, 0x68, 0x89, 0x54, 0x0b,
        0xa4, 0x0a, 0xa5, 0x6a, 0x95, 0x04, 0xad, 0x08, 0x6a, 0x44, 0xda, 0x04, 0x74, 0x05, 0xb0, 0x25, 0x54, 0x03
    };

    /** 闰月为大闰月（30天）的闰年年份 */
    private static final int[] BIG_LEAP_MONTH_YEAR = {
        6, 14, 19, 25, 33, 36, 38, 41, 44, 52, 55, 79, 117, 136, 147, 150, 155, 158, 185, 193
    };

    /** 节气表 */
    private static final char[][] SECTIONAL_TERM_MAP = {
        {
            7, 6, 6, 6, 6, 6, 6, 6, 6, 5, 6, 6, 6, 5, 5, 6, 6, 5, 5, 5, 5, 5, 5, 5, 5, 4, 5, 5
        }, {
            5, 4, 5, 5, 5, 4, 4, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4, 3, 4, 4, 4, 3, 3, 4, 4, 3, 3, 3
        }, {
            6, 6, 6, 7, 6, 6, 6, 6, 5, 6, 6, 6, 5, 5, 6, 6, 5, 5, 5, 6, 5, 5, 5, 5, 4, 5, 5, 5, 5
        }, {
            5, 5, 6, 6, 5, 5, 5, 6, 5, 5, 5, 5, 4, 5, 5, 5, 4, 4, 5, 5, 4, 4, 4, 5, 4, 4, 4, 4, 5
        }, {
            6, 6, 6, 7, 6, 6, 6, 6, 5, 6, 6, 6, 5, 5, 6, 6, 5, 5, 5, 6, 5, 5, 5, 5, 4, 5, 5, 5, 5
        }, {
            6, 6, 7, 7, 6, 6, 6, 7, 6, 6, 6, 6, 5, 6, 6, 6, 5, 5, 6, 6, 5, 5, 5, 6, 5, 5, 5, 5, 4, 5, 5, 5, 5
        }, {
            7, 8, 8, 8, 7, 7, 8, 8, 7, 7, 7, 8, 7, 7, 7, 7, 6, 7, 7, 7, 6, 6, 7, 7, 6, 6, 6, 7, 7
        }, {
            8, 8, 8, 9, 8, 8, 8, 8, 7, 8, 8, 8, 7, 7, 8, 8, 7, 7, 7, 8, 7, 7, 7, 7, 6, 7, 7, 7, 6, 6, 7, 7, 7
        }, {
            8, 8, 8, 9, 8, 8, 8, 8, 7, 8, 8, 8, 7, 7, 8, 8, 7, 7, 7, 8, 7, 7, 7, 7, 6, 7, 7, 7, 7
        }, {
            9, 9, 9, 9, 8, 9, 9, 9, 8, 8, 9, 9, 8, 8, 8, 9, 8, 8, 8, 8, 7, 8, 8, 8, 7, 7, 8, 8, 8
        }, {
            8, 8, 8, 8, 7, 8, 8, 8, 7, 7, 8, 8, 7, 7, 7, 8, 7, 7, 7, 7, 6, 7, 7, 7, 6, 6, 7, 7, 7
        }, {
            7, 8, 8, 8, 7, 7, 8, 8, 7, 7, 7, 8, 7, 7, 7, 7, 6, 7, 7, 7, 6, 6, 7, 7, 6, 6, 6, 7, 7
        }
    };

    /** 节气表 */
    private static final char[][] SECTIONAL_TERM_YEAR = {
        {
            13, 49, 85, 117, 149, 185, 201, 250, 250
        }, {
            13, 45, 81, 117, 149, 185, 201, 250, 250
        }, {
            13, 48, 84, 112, 148, 184, 200, 201, 250
        }, {
            13, 45, 76, 108, 140, 172, 200, 201, 250
        }, {
            13, 44, 72, 104, 132, 168, 200, 201, 250
        }, {
            5, 33, 68, 96, 124, 152, 188, 200, 201
        }, {
            29, 57, 85, 120, 148, 176, 200, 201, 250
        }, {
            13, 48, 76, 104, 132, 168, 196, 200, 201
        }, {
            25, 60, 88, 120, 148, 184, 200, 201, 250
        }, {
            16, 44, 76, 108, 144, 172, 200, 201, 250
        }, {
            28, 60, 92, 124, 160, 192, 200, 201, 250
        }, {
            17, 53, 85, 124, 156, 188, 200, 201, 250
        }
    };

    /** 中气表 */
    private static final char[][] PRINCIPLE_TERM_MAP = {
        {
            21, 21, 21, 21, 21, 20, 21, 21, 21, 20, 20, 21, 21, 20, 20, 20, 20, 20, 20, 20, 20, 19, 20, 20, 20, 19, 19, 20
        }, {
            20, 19, 19, 20, 20, 19, 19, 19, 19, 19, 19, 19, 19, 18, 19, 19, 19, 18, 18, 19, 19, 18, 18, 18, 18, 18, 18, 18
        }, {
            21, 21, 21, 22, 21, 21, 21, 21, 20, 21, 21, 21, 20, 20, 21, 21, 20, 20, 20, 21, 20, 20, 20, 20, 19, 20, 20, 20, 20
        }, {
            20, 21, 21, 21, 20, 20, 21, 21, 20, 20, 20, 21, 20, 20, 20, 20, 19, 20, 20, 20, 19, 19, 20, 20, 19, 19, 19, 20, 20
        }, {
            21, 22, 22, 22, 21, 21, 22, 22, 21, 21, 21, 22, 21, 21, 21, 21, 20, 21, 21, 21, 20, 20, 21, 21, 20, 20, 20, 21, 21
        }, {
            22, 22, 22, 22, 21, 22, 22, 22, 21, 21, 22, 22, 21, 21, 21, 22, 21, 21, 21, 21, 20, 21, 21, 21, 20, 20, 21, 21, 21
        }, {
            23, 23, 24, 24, 23, 23, 23, 24, 23, 23, 23, 23, 22, 23, 23, 23, 22, 22, 23, 23, 22, 22, 22, 23, 22, 22, 22, 22, 23
        }, {
            23, 24, 24, 24, 23, 23, 24, 24, 23, 23, 23, 24, 23, 23, 23, 23, 22, 23, 23, 23, 22, 22, 23, 23, 22, 22, 22, 23, 23
        }, {
            23, 24, 24, 24, 23, 23, 24, 24, 23, 23, 23, 24, 23, 23, 23, 23, 22, 23, 23, 23, 22, 22, 23, 23, 22, 22, 22, 23, 23
        }, {
            24, 24, 24, 24, 23, 24, 24, 24, 23, 23, 24, 24, 23, 23, 23, 24, 23, 23, 23, 23, 22, 23, 23, 23, 22, 22, 23, 23, 23
        }, {
            23, 23, 23, 23, 22, 23, 23, 23, 22, 22, 23, 23, 22, 22, 22, 23, 22, 22, 22, 22, 21, 22, 22, 22, 21, 21, 22, 22, 22
        }, {
            22, 22, 23, 23, 22, 22, 22, 23, 22, 22, 22, 22, 21, 22, 22, 22, 21, 21, 22, 22, 21, 21, 21, 22, 21, 21, 21, 21, 22
        }
    };

    /** 中气表 */
    private static final char[][] PRINCIPLE_TERM_YEAR = {
        {
            13, 45, 81, 113, 149, 185, 201
        }, {
            21, 57, 93, 125, 161, 193, 201
        }, {
            21, 56, 88, 120, 152, 188, 200, 201
        }, {
            21, 49, 81, 116, 144, 176, 200, 201
        }, {
            17, 49, 77, 112, 140, 168, 200, 201
        }, {
            28, 60, 88, 116, 148, 180, 200, 201
        }, {
            25, 53, 84, 112, 144, 172, 200, 201
        }, {
            29, 57, 89, 120, 148, 180, 200, 201
        }, {
            17, 45, 73, 108, 140, 168, 200, 201
        }, {
            28, 60, 92, 124, 160, 192, 200, 201
        }, {
            16, 44, 80, 112, 148, 180, 200, 201
        }, {
            17, 53, 88, 120, 156, 188, 200, 201
        }
    };

    // 初始日，公历农历对应日期：
    // 公历 1901 年 1 月 1 日，对应农历 4598 年 11 月 11 日
    private static final int BASE_YEAR = 1901;

    private static final int BASE_MONTH = 1;

    private static final int BASE_DATE = 1;

    private static final int BASE_INDEX = 0;

    private static final int BASE_CHINESE_YEAR = 4598 - 1;

    private static final int BASE_CHINESE_MONTH = 11;

    private static final int BASE_CHINESE_DAY = 11;

    /** 阳历年 */
    private int gregorianYear = DEFAULT_VALUE;

    /** 阳历月 */
    private int gregorianMonth = DEFAULT_VALUE;

    /** 阳历日 */
    private int gregorianDate = DEFAULT_VALUE;

    private int dayOfYear = DEFAULT_VALUE;

    /** 周日为一星期的第一天,合法值为1~7 */
    private int dayOfWeek = DEFAULT_VALUE;

    /** 农历年号 */
    private int chineseYear = DEFAULT_VALUE;

    /** 农历月号，负数表示闰月，绝对值在1~12之间 */
    private int chineseMonth = 0;

    /** 农历日 */
    private int chineseDate = DEFAULT_VALUE;

    /** 节气 */
    private int sectionalTerm = DEFAULT_VALUE;

    /** 中气 */
    private int principleTerm = DEFAULT_VALUE;

    private Calendar calendar;

    public FullCalendar(Context context, Calendar calendar, boolean needLunar) throws DateOutOfRangeException {
        this(context, calendar, needLunar, needLunar, true, true);
    }

    public FullCalendar(Context context, boolean needLunar) throws DateOutOfRangeException {
        this(context, needLunar, needLunar, true, true);
    }

    public FullCalendar(Context context, boolean needLunarDate, boolean needSolarTime, boolean needDayOfWeek, boolean needDayOfYear) throws DateOutOfRangeException {
        calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        init(context, calendar, needLunarDate, needSolarTime, needDayOfWeek, needDayOfYear);
    }

    /**
     * 初始化农历
     * @param year 阳历年
     * @param month 阳历月，1~12
     * @param day 阳历日
     * @throws DateOutOfRangeException
     */
    public FullCalendar(Context context, Calendar calendar, boolean needLunarDate, boolean needSolarTime, boolean needDayOfWeek, boolean needDayOfYear) throws DateOutOfRangeException {
        init(context, calendar, needLunarDate, needSolarTime, needDayOfWeek, needDayOfYear);
    }

    private void init(Context context, Calendar calendar, boolean needChineseField, boolean needSolarTime, boolean needDayOfWeek, boolean needDayOfYear) throws DateOutOfRangeException {
        this.context = context;
        synchronized (this) {

            mChineseHolidayTab = new Holiday[] {
                new Holiday(12, 30, R.string.lunar_holiday_1230), new Holiday(1, 1, R.string.lunar_holiday_0101), new Holiday(1, 15, R.string.lunar_holiday_0115),
                new Holiday(5, 5, R.string.lunar_holiday_0505), new Holiday(7, 7, R.string.lunar_holiday_0707), new Holiday(8, 15, R.string.lunar_holiday_0815),
                new Holiday(9, 9, R.string.lunar_holiday_0909), new Holiday(12, 8, R.string.lunar_holiday_1208), new Holiday(12, 23, R.string.lunar_holiday_1223)
            };

        }
        this.calendar = calendar;
        gregorianYear = calendar.get(Calendar.YEAR);
        gregorianMonth = calendar.get(Calendar.MONTH) + 1;
        gregorianDate = calendar.get(Calendar.DAY_OF_MONTH);

        checkYearRange(gregorianYear);

        if (needChineseField) {
            computeLunarDate();
        }
        if (needSolarTime) {
            computeSolarTerms();
        }
        if (needDayOfWeek) {
            dayOfWeek = dayOfWeek(gregorianYear, gregorianMonth, gregorianDate);
        }
        if (needDayOfYear) {
            dayOfYear = dayOfWeek(gregorianYear, gregorianMonth, gregorianDate);
        }

    }

    public Date getDate() {
        return calendar.getTime();
    }

    private boolean isGregorianLeapYear(int year) {
        boolean isLeap = false;
        if (year % 4 == 0) {
            isLeap = true;
        }
        if (year % 100 == 0) {
            isLeap = false;
        }
        if (year % 400 == 0) {
            isLeap = true;
        }
        return isLeap;
    }

    private int dayOfYear(int y, int m, int d) {
        int c = 0;
        for (int i = 1; i < m; i++) {
            c = c + daysInGregorianMonth(y, i);
        }
        c = c + d;
        return c;
    }

    private int dayOfWeek(int y, int m, int d) {
        int w = 1; // 公历一年一月一日是星期一，所以起始值为星期日
        y = (y - 1) % 400 + 1; // 公历星期值分部 400 年循环一次
        int ly = (y - 1) / 4; // 闰年次数
        ly = ly - (y - 1) / 100;
        ly = ly + (y - 1) / 400;
        int ry = y - 1 - ly; // 常年次数
        w = w + ry; // 常年星期值增一
        w = w + 2 * ly; // 闰年星期值增二
        w = w + dayOfYear(y, m, d);
        w = (w - 1) % 7 + 1;
        return w;
    }

    private int daysInGregorianMonth(int y, int m) {
        int d = DAYS_IN_GREGORIAN_MONTH[m - 1];
        if (m == 2 && isGregorianLeapYear(y)) {
            d++; // 公历闰年二月多一天
        }
        return d;
    }

    /**
     * 计算农历日期
     * @return
     * @throws DateOutOfRangeException
     */
    private int computeLunarDate() throws DateOutOfRangeException {
        checkYearRange(gregorianYear);
        int startYear = BASE_YEAR;
        int startMonth = BASE_MONTH;
        int startDate = BASE_DATE;
        chineseYear = BASE_CHINESE_YEAR;
        chineseMonth = BASE_CHINESE_MONTH;
        chineseDate = BASE_CHINESE_DAY;
        // 第二个对应日，用以提高计算效率
        // 公历 2011 年 1 月 1 日，对应农历 4708 年 11 月 27 日
        if (gregorianYear >= 2011) {
            startYear = BASE_YEAR + 110;
            startMonth = 1;
            startDate = 1;
            chineseYear = BASE_CHINESE_YEAR + 110;
            chineseMonth = 11;
            chineseDate = 27;
        }
        int daysDiff = 0;
        for (int i = startYear; i < gregorianYear; i++) {
            daysDiff += 365;
            if (isGregorianLeapYear(i)) {
                daysDiff += 1; // leap year
            }
        }
        for (int i = startMonth; i < gregorianMonth; i++) {
            daysDiff += daysInGregorianMonth(gregorianYear, i);
        }
        daysDiff += gregorianDate - startDate;

        chineseDate += daysDiff;
        int lastDate = daysInChineseMonth(chineseYear, chineseMonth);
        int nextMonth = nextChineseMonth(chineseYear, chineseMonth);
        while (chineseDate > lastDate) {
            if (Math.abs(nextMonth) < Math.abs(chineseMonth)) {
                chineseYear++;
            }
            chineseMonth = nextMonth;
            chineseDate -= lastDate;
            lastDate = daysInChineseMonth(chineseYear, chineseMonth);
            nextMonth = nextChineseMonth(chineseYear, chineseMonth);
        }
        return 0;
    }

    private int daysInChineseMonth(int y, int m) {
        // 注意：闰月 m < 0
        int index = y - BASE_CHINESE_YEAR + BASE_INDEX;
        int v = 0;
        int l = 0;
        int d = 30;
        if (1 <= m && m <= 8) {
            v = CHINESE_MONTH_INFO[2 * index];
            l = m - 1;
            if (((v >> l) & 0x01) == 1) {
                d = 29;
            }
        } else if (9 <= m && m <= 12) {
            v = CHINESE_MONTH_INFO[2 * index + 1];
            l = m - 9;
            if (((v >> l) & 0x01) == 1) {
                d = 29;
            }
        } else {
            v = CHINESE_MONTH_INFO[2 * index + 1];
            v = (v >> 4) & 0x0F;
            if (v != Math.abs(m)) {
                d = 0;
            } else {
                d = 29;
                for (int i = 0; i < BIG_LEAP_MONTH_YEAR.length; i++) {
                    if (BIG_LEAP_MONTH_YEAR[i] == index) {
                        d = 30;
                        break;
                    }
                }
            }
        }
        return d;
    }

    private int nextChineseMonth(int y, int m) {
        int n = Math.abs(m) + 1;
        if (m > 0) {
            int index = y - BASE_CHINESE_YEAR + BASE_INDEX;
            int v = CHINESE_MONTH_INFO[2 * index + 1];
            v = (v >> 4) & 0x0F;
            if (v == m) {
                n = -m;
            }
        }
        if (n == 13) {
            n = 1;
        }
        return n;
    }

    private int computeSolarTerms() throws DateOutOfRangeException {
        checkYearRange(gregorianYear);
        sectionalTerm = sectionalTerm(gregorianYear, gregorianMonth);
        principleTerm = principleTerm(gregorianYear, gregorianMonth);
        return 0;
    }

    private int sectionalTerm(int y, int m) throws DateOutOfRangeException {
        checkYearRange(y);
        int index = 0;
        int ry = y - BASE_YEAR + 1;
        while (ry >= SECTIONAL_TERM_YEAR[m - 1][index]) {
            index++;
        }
        int term = SECTIONAL_TERM_MAP[m - 1][4 * index + ry % 4];
        if ((ry == 0) && (m == 1)) {
            term = 6;
        } else if ((ry == 17) && (m == 12)) {
            term = 8;
        } else if ((ry == 27) && (m == 9)) {
            term = 9;
        } else if ((ry == 114) && (m == 3)) {
            term = 6;
        }

        return term;
    }

    private int principleTerm(int y, int m) throws DateOutOfRangeException {
        checkYearRange(y);
        int index = 0;
        int ry = y - BASE_YEAR + 1;
        while (ry >= PRINCIPLE_TERM_YEAR[m - 1][index]) {
            index++;
        }
        int term = PRINCIPLE_TERM_MAP[m - 1][4 * index + ry % 4];
        if ((ry == 12) && (m == 11)) {
            term = 22;
        } else if ((ry == 13) && (m == 9)) {
            term = 23;
        } else if ((ry == 23) && (m == 2)) {
            term = 20;
        } else if ((ry == 28) && (m == 6)) {
            term = 22;
        } else if ((ry == 151) && (m == 3)) {
            term = 20;
        } else if ((ry == 184) && (m == 3)) {
            term = 19;
        }
        return term;
    }

    private void checkYearRange(int y) throws DateOutOfRangeException {
        if (y < CALENDAR_BOTTOM_YEAR || y > CALENDAR_TOP_YEAR) {
            throw new DateOutOfRangeException();
        }
    }

    /**
     * 获取阳历年
     * @return
     */
    public int getGregorianYear() {
        return gregorianYear;
    }

    /**
     * 获取阳历月（1~12）
     * @return
     */
    public int getGregorianMonth() {
        return gregorianMonth;
    }

    /**
     * 获取阳历日期
     * @return
     */
    public int getGregorianDate() {
        return gregorianDate;
    }

    /**
     * 获取所在本年的天数
     * @return
     */
    public int getDayOfYear() {
        if (dayOfYear == DEFAULT_VALUE) {
            throw new RuntimeException("Please making param \"needDayOfYear\" true in constructor");
        }
        return dayOfYear;
    }

    /**
     * 获取dayOfWeek. 1~7,从周日开始
     * @return
     */
    public int getDayOfWeek() {
        if (dayOfWeek == DEFAULT_VALUE) {
            throw new RuntimeException("Please making param \"needDayOfWeek\" true in constructor");
        }
        return dayOfWeek;
    }

    /**
     * 获取星期的字符串
     * @return
     */
    public String getDayOfWeekString() {
        if (dayOfWeek == DEFAULT_VALUE) {
            throw new RuntimeException("Please making param \"needDayOfWeek\" true in constructor");
        }
        return getWeekNameString(dayOfWeek - 1);
    }

    /**
     * 获取农历月份 1~12
     * @return
     */
    public int getChineseMonth() {
        if (chineseMonth == 0) {
            throw new RuntimeException("Please making param \"needLunarDate\" true in constructor");
        }
        return chineseMonth;
    }

    /**
     * 获取农历日期
     * @return
     */
    public int getChineseDate() {
        if (chineseDate == DEFAULT_VALUE) {
            throw new RuntimeException("Please making param \"needLunarDate\" true in constructor");
        }
        return chineseDate;
    }

    /**
     * 获取农历日期的名称
     * @return
     */
    public String getChineseShortDateName() {
        if (chineseDate == DEFAULT_VALUE) {
            throw new RuntimeException("Please making param \"needLunarDate\" true in constructor");
        }

        if (chineseDate > 30) {
            return "";
        }

        if (chineseDate == 10) {
            return getDatePrefixNameString(0) + getChineseNumString(9); // 初十
        }
        if (chineseDate == 20) {
            return getChineseNumString(1) + getChineseNumString(9); // 二十
        }
        if (chineseDate == 30) {
            return getChineseNumString(2) + getChineseNumString(9); // 三十
        }

        return getDatePrefixNameString(chineseDate / 10) + getChineseNumString(chineseDate % 10 - 1);
    }

    /**
     * 获取农历月份的名称
     * @return
     */
    public String getChineseShortMonthName() {
        if (chineseMonth == 0) {
            throw new RuntimeException("Please making param \"needLunarDate\" true in constructor");
        }

        return new StringBuilder(chineseMonth < 0 ? getLeapString() : "").append(getChineseMonthString(Math.abs(chineseMonth) - 1)).append(getMonthString()).toString();
    }

    /**
     * 获取农历月份和日期的完整名称
     * @return
     */
    public String getChineseFullDateName() {
        return getChineseShortMonthName() + getChineseShortDateName();
    }

    /**
     * 获取节气名称。没有则返回空
     * @return
     */
    public String getJieqiName() {
        if (chineseDate == DEFAULT_VALUE) {
            throw new RuntimeException("Please making param \"needLunarDate\" true in constructor");
        }

        if (gregorianDate == sectionalTerm) {
            return getSectionalTermString(gregorianMonth - 1);
        } else if (gregorianDate == principleTerm) {
            return getPrincipleTermString(gregorianMonth - 1);
        }

        return null;
    }

    /**
     * 获取农历假期的名称。没有则返回空
     * @return
     */
    public String getChineseHoliday() {
        for (Holiday holiday : mChineseHolidayTab) {
            if (chineseMonth == holiday.month && chineseDate == holiday.day) {
                return context.getString(holiday.nameResId);
            }
        }
        return null;
    }

    public String getI18nDate() {
        SimpleDateFormat format = new SimpleDateFormat("MM" + getMonthString() + "dd" + getDayString());
        return format.format(getDate());
    }

    /** 十个中文数字 */
    private String getChineseNumString(int index) {
        return getStringResource(R.array.cn_num, index);
    }

    /** 日期前缀，初，十，廿 */
    private String getDatePrefixNameString(int index) {
        return getStringResource(R.array.lunar_date_prefix, index);
    }

    /** 农历月常量 */
    private String getChineseMonthString(int index) {
        return getStringResource(R.array.lunar_monthname, index);
    }

    /** 中气常量 */
    private String getPrincipleTermString(int index) {
        return getStringResource(R.array.lunar_principle, index);
    }

    /** 节气常量 */
    private String getSectionalTermString(int index) {
        return getStringResource(R.array.lunar_sectional, index);
    }

    /** 一周的名称，从周日开始 */
    private String getWeekNameString(int index) {
        return getStringResource(R.array.date_week, index);
    }

    /** 闰 */
    private String getLeapString() {
        return context.getString(R.string.lunar_leap);
    }

    private String getMonthString() {
        return context.getString(R.string.date_unit_month);
    }

    private String getDayString() {
        return context.getString(R.string.date_unit_day);
    }

    private String getStringResource(int resId, int index) {
        String[] strings = context.getResources().getStringArray(resId);
        if (strings != null && strings.length > index) {
            return strings[index];
        }
        return null;
    }

    static class Holiday {
        int month;

        int day;

        int nameResId;

        public Holiday(int month, int day, int nameResId) {
            super();
            this.month = month;
            this.day = day;
            this.nameResId = nameResId;
        }

    }

    public class DateOutOfRangeException extends Exception {

        private static final long serialVersionUID = 8308140376473626752L;

    }

}
