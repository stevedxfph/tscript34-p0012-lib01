require 'digest/md5'
require 'digest/sha1'

for f in Dir.glob("target/*.{jar,pom}")
  File.write("#{f}.md5", Digest::MD5.file(f).hexdigest)
  File.write("#{f}.sha1", Digest::SHA1.file(f).hexdigest)
end
