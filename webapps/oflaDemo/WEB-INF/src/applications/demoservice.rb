# JRuby - style
require 'java'
module RedFive
    include_package "org.springframework.core.io"
end
include_class "org.red5.server.api.Red5"
include_class "java.util.HashMap"

#
# demoservice.rb - a translation into Ruby of the olfa demo application, a red5 example.
#
# @author Paul Gregoire
#
class DemoService

    attr_reader :filesMap
    attr_writer :filesMap

	def initialize
	   puts "Initializing ruby demoservice"
	   @filesMap = HashMap.new
	end

	def getListOfAvailableFLVs
		puts "Getting the FLV files"
		begin
		    #puts "R5 con local: #{Red5::getConnectionLocal}"
		    #puts "Scope: #{Red5::getConnectionLocal.getScope}"
			flvs = Red5::getConnectionLocal.getScope.getResources("streams/*.flv")
			for flv in flvs
				file = flv.getFile
				lastModified = formatDate(Time.at(file.lastModified))
				#File.mtime("testfile")
				flvName = file.getName
				flvBytes = file.length
				#File.size("testfile")
				puts "FLV Name: #{flvName}"
				puts "Last modified date: #{lastModified}"
				puts "Size: #{flvBytes}"
				puts "-------"
				fileInfo = HashMap.new
				fileInfo["name"] = flvName
				fileInfo["lastModified"] = lastModified
				fileInfo["size"] = flvBytes
				@filesMap[flvName] = fileInfo
			end
		rescue
			puts "Error in getListOfAvailableFLVs"
		end
		return filesMap
	end

	def formatDate(date)
		return date.strftime("%d/%m/%Y %I:%M:%S")
	end

    def method_missing(m, *args)
      super unless @value.respond_to?(m) 
      return @value.send(m, *args)
    end

end
