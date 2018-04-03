module Jekyll
  class IncludeFileTag < Liquid::Tag
    def initialize(tag_name, text, tokens)
      super
      @text = text.strip
    end
    def render(context)
      if Dir.pwd.end_with?("jekyll")
        # running jekyll inside the top level directory of the jekyll theme directory
        tmpl = File.read File.join Dir.pwd, @text
      else
        # running jekyll using gradle
        tmpl = File.read File.join Dir.pwd, 'src/main/jekyll/', @text
      end
      # simply return the text, as is, from the file
      tmpl
    end
  end
end
Liquid::Template.register_tag('includefile', Jekyll::IncludeFileTag)
