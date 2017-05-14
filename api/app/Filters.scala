
import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter

class Filters @Inject() (corsFilter: CORSFilter, loggingFilter: LoggingFilter)
  extends DefaultHttpFilters(corsFilter, loggingFilter)

