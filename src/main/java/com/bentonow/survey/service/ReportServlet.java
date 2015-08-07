package com.bentonow.survey.service;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bentonow.survey.model.Entity;

@WebServlet(urlPatterns={"/report"})
public class ReportServlet extends HttpServlet {
  private static final long serialVersionUID = 8747329166514244513L;

  private static final Pattern datePattern = Pattern.compile("([0-9]{4})-([0-9]{1,2})-([0-9]{1,2})");

  private static String formatDate(final String parameter) {
    if (parameter == null)
      return null;

    final Matcher matcher = datePattern.matcher(parameter);
    if (!matcher.matches())
      throw new IllegalArgumentException("date parameter does not match pattern yyyy-mm-dd");

    final String year = matcher.group(1);
    final String month = matcher.group(2);
    final String date = matcher.group(3);
    return year + "-" + (month.length() == 1 ? "0" + month : month) + "-" + (date.length() == 1 ? "0" + date : date);
  }

  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final String fromParameter = ReportServlet.formatDate(request.getParameter("f"));
    final String toParameter = ReportServlet.formatDate(request.getParameter("t"));

    final StringWriter out = new StringWriter();
    if (fromParameter == null || toParameter == null) {
      out.append("<html>\n");
      out.append("  <head>\n");
      out.append("    <title>Report</title>\n");
      out.append("    <style>.datepickr-wrapper{display:inline-block;position:relative}.datepickr-calendar{font-family:'Trebuchet MS',Tahoma,Verdana,Arial,sans-serif;font-size:12px;background-color:#eee;color:#333;border:1px solid #ddd;-moz-border-radius:4px;-webkit-border-radius:4px;border-radius:4px;padding:2px;display:none;position:absolute;top:100%;left:0;z-index:100}.open .datepickr-calendar{display:block}.datepickr-calendar .datepickr-months{background-color:#f6af3a;border:1px solid #e78f08;-moz-border-radius:4px;-webkit-border-radius:4px;border-radius:4px;color:#fff;padding:2px;text-align:center;font-size:120%}.datepickr-calendar .datepickr-next-month,.datepickr-calendar .datepickr-prev-month{color:#fff;text-decoration:none;padding:0 .4em;-moz-border-radius:4px;-webkit-border-radius:4px;border-radius:4px;cursor:pointer}.datepickr-calendar .datepickr-prev-month{float:left}.datepickr-calendar .datepickr-next-month{float:right}.datepickr-calendar .datepickr-current-month{padding:0 .5em}.datepickr-calendar .datepickr-next-month:hover,.datepickr-calendar .datepickr-prev-month:hover{background-color:#fdf5ce;color:#c77405}.datepickr-calendar table{border-collapse:collapse;padding:0;width:100%}.datepickr-calendar thead{font-size:90%}.datepickr-calendar td,.datepickr-calendar th{width:14.3%}.datepickr-calendar th{text-align:center;padding:5px}.datepickr-calendar td{text-align:right;padding:1px}.datepickr-calendar .datepickr-day{display:block;color:#1c94c4;background-color:#f6f6f6;border:1px solid #ccc;padding:5px;cursor:pointer}.datepickr-calendar .datepickr-day:hover{color:#C77405;background-color:#fdf5ce;border:1px solid #fbcb09}.datepickr-calendar .today .datepickr-day{background-color:#fff0A5;border:1px solid #fed22f;color:#363636}.datepickr-calendar .selected .datepickr-day{background-color:#1c94c4;color:#f6f6f6}.datepickr-calendar .disabled .datepickr-day,.datepickr-calendar .disabled .datepickr-day:hover{background-color:#eee;border:1px dotted #ccc;color:#bbb;cursor:default} body { font-family: Verdana; } h1 { font-size: 25px; } .calendar-icon { display: inline-block; vertical-align: middle; width: 32px; height: 32px; background: url(images/calendar.png); } input { border: 1px solid #aaa; padding: 5px; width: 300px; }</style>");
      out.append("  </head>\n");
      out.append("  <body>\n");
      out.append("    <h3>Report:</h3>\n");
      out.append("    <form action='/report' method='get'>\n");
      out.append("      <div class=\"datepickr-wrapper\"><input class=\"datepickr\" placeholder=\"From\" name='f'><div class=\"datepickr-calendar\"><div class=\"datepickr-months\"><span class=\"datepickr-prev-month\">&lt;</span><span class=\"datepickr-next-month\">&gt;</span><span class=\"datepickr-current-month\">August 2015</span></div><table><thead><tr><th>Sun</th><th>Mon</th><th>Tue</th><th>Wed</th><th>Thu</th><th>Fri</th><th>Sat</th></tr></thead><tbody><tr><td colspan=\"6\">&nbsp;</td><td class=\"\"><span class=\"datepickr-day\">1</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">2</span></td><td class=\"\"><span class=\"datepickr-day\">3</span></td><td class=\"\"><span class=\"datepickr-day\">4</span></td><td class=\"\"><span class=\"datepickr-day\">5</span></td><td class=\"\"><span class=\"datepickr-day\">6</span></td><td class=\" today\"><span class=\"datepickr-day\">7</span></td><td class=\"\"><span class=\"datepickr-day\">8</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">9</span></td><td class=\"\"><span class=\"datepickr-day\">10</span></td><td class=\"\"><span class=\"datepickr-day\">11</span></td><td class=\"\"><span class=\"datepickr-day\">12</span></td><td class=\"\"><span class=\"datepickr-day\">13</span></td><td class=\"\"><span class=\"datepickr-day\">14</span></td><td class=\"\"><span class=\"datepickr-day\">15</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">16</span></td><td class=\"\"><span class=\"datepickr-day\">17</span></td><td class=\"\"><span class=\"datepickr-day\">18</span></td><td class=\"\"><span class=\"datepickr-day\">19</span></td><td class=\"\"><span class=\"datepickr-day\">20</span></td><td class=\"\"><span class=\"datepickr-day\">21</span></td><td class=\"\"><span class=\"datepickr-day\">22</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">23</span></td><td class=\"\"><span class=\"datepickr-day\">24</span></td><td class=\"\"><span class=\"datepickr-day\">25</span></td><td class=\"\"><span class=\"datepickr-day\">26</span></td><td class=\"\"><span class=\"datepickr-day\">27</span></td><td class=\"\"><span class=\"datepickr-day\">28</span></td><td class=\"\"><span class=\"datepickr-day\">29</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">30</span></td><td class=\"\"><span class=\"datepickr-day\">31</span></td></tr></tbody></table></div></div>");
      out.append("      <div class=\"datepickr-wrapper\"><input class=\"datepickr\" placeholder=\"To\" name='t'><div class=\"datepickr-calendar\"><div class=\"datepickr-months\"><span class=\"datepickr-prev-month\">&lt;</span><span class=\"datepickr-next-month\">&gt;</span><span class=\"datepickr-current-month\">August 2015</span></div><table><thead><tr><th>Sun</th><th>Mon</th><th>Tue</th><th>Wed</th><th>Thu</th><th>Fri</th><th>Sat</th></tr></thead><tbody><tr><td colspan=\"6\">&nbsp;</td><td class=\"\"><span class=\"datepickr-day\">1</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">2</span></td><td class=\"\"><span class=\"datepickr-day\">3</span></td><td class=\"\"><span class=\"datepickr-day\">4</span></td><td class=\"\"><span class=\"datepickr-day\">5</span></td><td class=\"\"><span class=\"datepickr-day\">6</span></td><td class=\" today\"><span class=\"datepickr-day\">7</span></td><td class=\"\"><span class=\"datepickr-day\">8</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">9</span></td><td class=\"\"><span class=\"datepickr-day\">10</span></td><td class=\"\"><span class=\"datepickr-day\">11</span></td><td class=\"\"><span class=\"datepickr-day\">12</span></td><td class=\"\"><span class=\"datepickr-day\">13</span></td><td class=\"\"><span class=\"datepickr-day\">14</span></td><td class=\"\"><span class=\"datepickr-day\">15</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">16</span></td><td class=\"\"><span class=\"datepickr-day\">17</span></td><td class=\"\"><span class=\"datepickr-day\">18</span></td><td class=\"\"><span class=\"datepickr-day\">19</span></td><td class=\"\"><span class=\"datepickr-day\">20</span></td><td class=\"\"><span class=\"datepickr-day\">21</span></td><td class=\"\"><span class=\"datepickr-day\">22</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">23</span></td><td class=\"\"><span class=\"datepickr-day\">24</span></td><td class=\"\"><span class=\"datepickr-day\">25</span></td><td class=\"\"><span class=\"datepickr-day\">26</span></td><td class=\"\"><span class=\"datepickr-day\">27</span></td><td class=\"\"><span class=\"datepickr-day\">28</span></td><td class=\"\"><span class=\"datepickr-day\">29</span></td></tr><tr><td class=\"\"><span class=\"datepickr-day\">30</span></td><td class=\"\"><span class=\"datepickr-day\">31</span></td></tr></tbody></table></div></div>");
      out.append("      <br/>\n");
      out.append("      <br/>\n");
      out.append("      <input type='submit' value='Report'/>\n");
      out.append("    </form>\n");
      out.append("    <script type=\"text/javascript\">var datepickr=function(d,c){var f,h,a=[],k;datepickr.prototype=datepickr.init.prototype;h=function(a){a._datepickr&&a._datepickr.destroy();a._datepickr=new datepickr.init(a,c);return a._datepickr};if(d.nodeName)return h(d);f=datepickr.prototype.querySelectorAll(d);if(1===f.length)return h(f[0]);for(k=0;k<f.length;k++)a.push(h(f[k]));return a};datepickr.init=function(d,c){var f,h,a=this,k={dateFormat:\"F j, Y\",altFormat:null,altInput:null,minDate:null,maxDate:null,shorthandCurrentMonth:!1},l=document.createElement(\"div\"),t=document.createElement(\"span\"),u=document.createElement(\"table\"),v=document.createElement(\"tbody\"),g,m=new Date,B,n,p,w,C,r,x,D,E,s,F,G,y,H,z,A,I;l.className=\"datepickr-calendar\";t.className=\"datepickr-current-month\";c=c||{};B=function(){g=document.createElement(\"div\");g.className=\"datepickr-wrapper\";a.element.parentNode.insertBefore(g,a.element);g.appendChild(a.element)};f={year:function(){return m.getFullYear()},month:{integer:function(){return m.getMonth()},string:function(a){var e=m.getMonth();return p(e,a)}},day:function(){return m.getDate()}};h={string:function(){return p(a.currentMonthView,a.config.shorthandCurrentMonth)},numDays:function(){return 1===a.currentMonthView&&(0===a.currentYearView%4&&0!==a.currentYearView%100||0===a.currentYearView%400)?29:a.l10n.daysInMonth[a.currentMonthView]}};n=function(b,e){var q=\"\",d=new Date(e),c={d:function(){var a=c.j();return 10>a?\"0\"+a:a},D:function(){return a.l10n.weekdays.shorthand[c.w()]},j:function(){return d.getDate()},l:function(){return a.l10n.weekdays.longhand[c.w()]},w:function(){return d.getDay()},F:function(){return p(c.n()-1,!1)},m:function(){var a=c.n();return 10>a?\"0\"+a:a},M:function(){return p(c.n()-1,!0)},n:function(){return d.getMonth()+1},U:function(){return d.getTime()/1E3},y:function(){return String(c.Y()).substring(2)},Y:function(){return d.getFullYear()}},f=b.split(\"\");a.forEach(f,function(a,b){c[a]&&\"\\\\\"!==f[b-1]?q+=c[a]():\"\\\\\"!==a&&(q+=a)});return q};p=function(b,e){return!0===e?a.l10n.months.shorthand[b]:a.l10n.months.longhand[b]};w=function(b,e,c,d){return b===d&&a.currentMonthView===e&&a.currentYearView===c};C=function(){var b=document.createElement(\"thead\"),e=a.l10n.firstDayOfWeek,c=a.l10n.weekdays.shorthand;0<e&&e<c.length&&(c=[].concat(c.splice(e,c.length),c.splice(0,e)));b.innerHTML=\"<tr><th>\"+c.join(\"</th><th>\")+\"</th></tr>\";u.appendChild(b)};r=function(){var b=(new Date(a.currentYearView,a.currentMonthView,1)).getDay(),c=h.numDays(),d=document.createDocumentFragment(),g=document.createElement(\"tr\"),k,l=\"\",p=\"\",m=\"\",n,b=b-a.l10n.firstDayOfWeek;0>b&&(b+=7);k=b;v.innerHTML=\"\";0<b&&(g.innerHTML+='<td colspan=\"'+b+'\">&nbsp;</td>');for(b=1;b<=c;b++){7===k&&(d.appendChild(g),g=document.createElement(\"tr\"),k=0);l=w(f.day(),f.month.integer(),f.year(),b)?\" today\":\"\";a.selectedDate&&(p=w(a.selectedDate.day,a.selectedDate.month,a.selectedDate.year,b)?\" selected\":\"\");if(a.config.minDate||a.config.maxDate)n=(new Date(a.currentYearView,a.currentMonthView,b)).getTime(),m=\"\",a.config.minDate&&n<a.config.minDate&&(m=\" disabled\"),a.config.maxDate&&n>a.config.maxDate&&(m=\" disabled\");g.innerHTML+='<td class=\"'+l+p+m+'\"><span class=\"datepickr-day\">'+b+\"</span></td>\";k++}d.appendChild(g);v.appendChild(d)};x=function(){t.innerHTML=h.string()+\" \"+a.currentYearView};D=function(){var a=document.createElement(\"div\");a.className=\"datepickr-months\";a.innerHTML='<span class=\"datepickr-prev-month\">&lt;</span><span class=\"datepickr-next-month\">&gt;</span>';a.appendChild(t);x();l.appendChild(a)};E=function(){0>a.currentMonthView&&(a.currentYearView--,a.currentMonthView=11);11<a.currentMonthView&&(a.currentYearView++,a.currentMonthView=0)};s=function(b){if(b.target!==a.element&&b.target!==g&&(b=b.target.parentNode,b!==g))for(;b!==g;)if(b=b.parentNode,null===b){A();break}};F=function(b){b=b.target;var c=b.className;c&&(\"datepickr-prev-month\"===c||\"datepickr-next-month\"===c?(\"datepickr-prev-month\"===c?a.currentMonthView--:a.currentMonthView++,E(),x(),r()):\"datepickr-day\"!==c||a.hasClass(b.parentNode,\"disabled\")||(a.selectedDate={day:parseInt(b.innerHTML,10),month:a.currentMonthView,year:a.currentYearView},b=(new Date(a.currentYearView,a.currentMonthView,a.selectedDate.day)).getTime(),a.config.altInput&&(a.config.altInput.value=a.config.altFormat?n(a.config.altFormat,b):n(a.config.dateFormat,b)),a.element.value=n(a.config.dateFormat,b),A(),r()))};G=function(){D();C();r();u.appendChild(v);l.appendChild(u);g.appendChild(l)};y=function(){return\"INPUT\"===a.element.nodeName?\"focus\":\"click\"};H=function(){a.addEventListener(a.element,y(),z);a.addEventListener(l,\"click\",F)};z=function(){a.addEventListener(document,\"click\",s);a.addClass(g,\"open\")};A=function(){a.removeEventListener(document,\"click\",s);a.removeClass(g,\"open\")};I=function(){var b,c;a.removeEventListener(document,\"click\",s);a.removeEventListener(a.element,y(),z);b=a.element.parentNode;b.removeChild(l);c=b.removeChild(a.element);b.parentNode.replaceChild(c,b)};(function(){var b,e;a.config={};a.destroy=I;for(b in k)a.config[b]=c[b]||k[b];a.element=d;a.element.value&&(e=Date.parse(a.element.value));e&&!isNaN(e)?(e=new Date(e),a.selectedDate={day:e.getDate(),month:e.getMonth(),year:e.getFullYear()},a.currentYearView=a.selectedDate.year,a.currentMonthView=a.selectedDate.month,a.currentDayView=a.selectedDate.day):(a.selectedDate=null,a.currentYearView=f.year(),a.currentMonthView=f.month.integer(),a.currentDayView=f.day());B();G();H()})();return a};datepickr.init.prototype={hasClass:function(d,c){return d.classList.contains(c)},addClass:function(d,c){d.classList.add(c)},removeClass:function(d,c){d.classList.remove(c)},forEach:function(d,c){[].forEach.call(d,c)},querySelectorAll:document.querySelectorAll.bind(document),isArray:Array.isArray,addEventListener:function(d,c,f,h){d.addEventListener(c,f,h)},removeEventListener:function(d,c,f,h){d.removeEventListener(c,f,h)},l10n:{weekdays:{shorthand:\"Sun Mon Tue Wed Thu Fri Sat\".split(\" \"),longhand:\"Sunday Monday Tuesday Wednesday Thursday Friday Saturday\".split(\" \")},months:{shorthand:\"Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec\".split(\" \"),longhand:\"January February March April May June July August September October November December\".split(\" \")},daysInMonth:[31,28,31,30,31,30,31,31,30,31,30,31],firstDayOfWeek:0}};</script>\n");
      out.append("    <script>datepickr('#datepickr'); datepickr('.datepickr', { dateFormat: 'Y-m-d'}); datepickr('#minAndMax', { minDate: new Date().getTime() - 2.592e8, maxDate: new Date().getTime() + 2.592e8 }); datepickr('.calendar-icon', { altInput: document.getElementById('calendar-input') }); datepickr('[title=\"parseMe\"]'); datepickr.prototype.l10n.months.shorthand = ['janv', 'févr', 'mars', 'avril', 'mai', 'juin', 'juil', 'août', 'sept', 'oct', 'nov', 'déc']; datepickr.prototype.l10n.months.longhand = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre']; datepickr.prototype.l10n.weekdays.shorthand = ['dim', 'lun', 'mar', 'mer', 'jeu', 'ven', 'sam']; datepickr.prototype.l10n.weekdays.longhand = ['dimanche', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi']; datepickr('#someFrench.sil-vous-plait', { dateFormat: '\\le j F Y' });</script>\n");
      out.append("  </body>\n");
      out.append("</html>\n");
    }
    else {
      String query1 = "SELECT DISTINCT\n";
      query1 += "  m.order_id,\n";
      query1 += "  m.created_on,\n";
      query1 += "  m.email,\n";
      query1 += "  ms.rating,\n";
      query1 += "  ms.comment\n";
      query1 += "FROM dish_survey ds, dish d, meal_survey ms, meal m\n";
      query1 += "WHERE ds.meal_id = m.id\n";
      query1 += "AND m.id = ms.meal_id\n";
      query1 += "AND ds.dish_id = d.id\n";
      query1 += "AND m.created_on >= CONVERT_TZ('" + fromParameter + " 00:00:00','America/Los_Angeles','UTC')\n";
      query1 += "AND m.created_on <= CONVERT_TZ('" + toParameter + " 23:59:59','America/Los_Angeles','UTC')\n";
      query1 += "ORDER BY m.created_on";

      final Map<Integer,String> mainRows = new HashMap<Integer,String>();
      try (
        final Connection connection = Entity.getConnection();
        final Statement selectStatement = connection.createStatement();
      ) {
        final ResultSet resultSet = selectStatement.executeQuery(query1);
        while (resultSet.next()) {
          final int oderId = resultSet.getInt(1);
          final int rating = resultSet.getInt(4);
          mainRows.put(oderId, "<tr><td nowrap><b>" + oderId + "</b></td><td nowrap><b>" + resultSet.getString(2) + "</b></td><td nowrap><b>" + resultSet.getString(3) + "</b></td><td align='right' nowrap><b>" + (rating < 8 ? "<font color='red'>" : "<font color='green'>") + rating + "</font></b></td><td nowrap><b>" + resultSet.getString(5) + "</b></td></tr>\n");
        }
      }
      catch (final SQLException e) {
        throw new ServletException(e);
      }

      String query = "SELECT\n";
      query += "  m.order_id,\n";
      query += "  d.name,\n";
      query += "  d.type,\n";
      query += "  ds.rating,\n";
      query += "  ds.comment\n";
      query += "FROM dish_survey ds, dish d, meal_survey ms, meal m\n";
      query += "WHERE ds.meal_id = m.id\n";
      query += "AND m.created_on >= CONVERT_TZ('" + fromParameter + " 00:00:00','America/Los_Angeles','UTC')\n";
      query += "AND m.created_on <= CONVERT_TZ('" + toParameter + " 23:59:59','America/Los_Angeles','UTC')\n";
      query += "AND m.id = ms.meal_id\n";
      query += "AND ds.dish_id = d.id\n";
      query += "ORDER BY m.created_on, d.type";

      out.append("<table border='1' width='100%'>\n");
      out.append("<tr>\n");
      out.append("<td nowrap><b><u>Order Id</u></b></td>\n");
      out.append("<td nowrap><b><u>Created On<br>Dish Name</u></b></td>\n");
      out.append("<td nowrap><b><u>Email<br>Dish Type</u></b></td>\n");
      out.append("<td nowrap><b><u>Rating</u></b></td>\n");
      out.append("<td><b><u>Comment</u></b></td>\n");
      out.append("</tr>\n");
      try (
        final Connection connection = Entity.getConnection();
        final Statement selectStatement = connection.createStatement();
      ) {
        int prevOrderId = Integer.MIN_VALUE;
        final ResultSet resultSet = selectStatement.executeQuery(query);
        while (resultSet.next()) {
          final int orderId = resultSet.getInt(1);
          if (prevOrderId != orderId) {
            prevOrderId = orderId;
            final String main = mainRows.get(orderId);
            if (main != null)
              out.append(main);
          }
          out.append("<tr>\n");
          out.append("<td></td>\n");
          out.append("<td nowrap>").append(resultSet.getString(2)).append("</td>\n");
          out.append("<td nowrap>").append(resultSet.getString(3)).append("</td>\n");
          final int rating = resultSet.getInt(4);
          out.append("<td align='right' nowrap>").append(rating == 0 ? "<font color='red'>" : "<font color='green'>").append("" + rating).append("</font>").append("</td>\n");
          out.append("<td>").append(resultSet.getString(5)).append("</td>\n");
          out.append("</tr>\n");
        }
      }
      catch (final SQLException e) {
        throw new ServletException(e);
      }

      out.append("</table>\n");
    }

    response.getOutputStream().write(out.toString().getBytes());
  }
}