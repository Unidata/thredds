## Documentation Theme for Jekyll

<http://idratherbewriting.com/documentation-theme-jekyll/index.html>

## General Kramdown Syntax

<https://kramdown.gettalong.org/syntax.html>

## Front Matter

Each markdown file needs to start with some front matter:

~~~md
title: Documentation Guide
last_updated: 2016-09-27
sidebar: tdsTutorial_sidebar
toc: false
permalink: doc_guide.html
~~~

## Headers

All headings start off as level 2.
The `title` set in the front matter will be displayed at the top of the rendered html page and uses the level 1 heading.
Headings, and their level number, are denoted by the number of hashtags on a line:

# level 1
## level 2
### level 3

~~~md
# level 1
## level 2
### level 3
~~~

## netCDF-Java, TDS jargon

italicize TDS specific jargon on first use:

_catalog roots_

_client catalog_

_configuration catalog_

~~~md
_catalog roots_

_client catalog_

_configuration catalog_
~~~

inline text html, xml elements, properties, code variables, snippits, file paths, surround in back ticks, like this:

`<catalog>`

~~~md
`<catalog>`
~~~

## Keeping git diffs clean

One line of text per line in file
blank line needed for new paragraph, so no formatting issue in doing this.
this keeps git diff clean

~~~md
One line of text per line in file
blank line needed for new paragraph, so no formatting issue in doing this.
this keeps git diff clean
~~~

## links

General format is:

~~~md
[text of link](url){:target="_blank"}
~~~

When linking between markdown files, the `url` will be the permalink defined in the front matter of the markdown file you wish to link to.

## highlight blocks

These templates under the top level `_includes/` directory.
Here is what we have right now:

{%include troubleshooting.html content="
Troubleshooting block.
" %}

{%include note.html content="
Note block.
" %}

{%include important.html content="
Important block.
" %}

{%include question.html content="
Question block.
" %}

{%include ahead.html content="
Thinking ahead block
" %}

```
{%raw%}{%include troubleshooting.html content="
Troubleshooting block.
" %}{%endraw%}

{%raw%}{%include note.html content="
Note block.
" %}{%endraw%}

{%raw%}{%include important.html content="
Important block.
" %}{%endraw%}

{%raw%}{%include question.html content="
Question block.
" %}{%endraw%}

{%raw%}{%include ahead.html content="
Thinking ahead block
" %}{%endraw%}
```

## images

Image

```
{%raw%}{% include image.html file="<location of image starting at images/>" alt="alt text" caption="caption" %}{%endraw%}
```

Note that you must always include a caption, even if it is empty.
For example, to link to the image `$PATH_TO_GIT_REPO/thredds-doc/images/sl_website-under-construction.jpeg`:

{% include image.html file="sl_website-under-construction.jpeg" alt="alt text" caption="" %}

~~~md
{%raw%}{% include image.html file="sl_website-under-construction.jpeg" alt="alt text" caption="" %}{%endraw%}
~~~


## sidebars

Sidebar information can be found under `_data/sidebars/`.
For example, the sidebar used in the TDS Tutorial is `_data/sidebars/tdsTutorial_sidebar.yml`.
If you add a new sidebar, you will need to make sure to add it to the `_config.yml` file at the top level of the docs directory, as well as the sidebar config, located at `_includes/custom/sidebarconfigs.html`

## Indenting text, code blocks, etc. in a list:

1. Restart Tomcat so the TDS is reinitialized:

   ~~~bash
   $ cd ${tomcat_home}/bin
   $ ./shutdown.sh
   $ ./startup.sh
   ~~~

   ~~~~~md
   ~~~bash
   $ cd ${tomcat_home}/bin
   $ ./shutdown.sh
   $ ./startup.sh
   ~~~
   ~~~~~

   All indented text are aligned with the "R" in "Restart", which is 3 spaces.
   Alignment is with the start of the text in the bullet of the list.
   This works the same way with nested lists as w

2. Using the `~` character in code blocks

   In order to get the `~~~bash~~~` and `~~~` to show up in the code block above, I had to do the following:

   ~~~~~~~~md
   ~~~~~md
   ~~~bash
   $ cd ${tomcat_home}/bin
   $ ./shutdown.sh
   $ ./startup.sh
   ~~~
   ~~~~~
   ~~~~~~~~

   Whatever hightlighting you would like, just make sure it has more `~` characters than any nested code blocks.

## Include content contained in external file

In order to link the same content in multiple tutorials or documentation sets, you need to keep the actual content in a separate file that the markdown file containing the frontmatter.
Take this guide for example.
This guide is used in both the netCDF-Java tutorial and the TDS Tutorial.
In order to do this, we need to create a markdown for each tutorial.
For example, `pages/tds_tutorial/developer/DocGuide.md`

~~~md
---
title: Documentation Guide
last_updated: 2017-02-17
sidebar: tdsTutorial_sidebar
toc: false
permalink: tds_doc_guide.html
---
{% raw %}
{% capture rmd %}{% includefile pages/netcdfJava/developer/DocGuide.inc %}{% endcapture %}
{{ rmd | liquify | markdownify }}
{% endraw %}
~~~

and `pages/netcdfJava/developer/DocGuide.md`

~~~md
---
title: Documentation Guide
last_updated: 2017-02-17
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: ncj_doc_guide.html
---
{% raw %}
{% capture rmd %}{% includefile pages/netcdfJava/developer/DocGuide.inc %}{% endcapture %}
{{ rmd | liquify | markdownify }}
{% endraw %}
~~~

Each markdown file contains both identical (title, last_update, toc) as well as unique (sidebar, permalink) frontmatter, but references the same actual content, which is contained within the file `pages/netcdfJava/developer/DocGuide.inc`
This is the only way to have the same information contained within different tutorials, while preserving the tutorial sidebar.

Yes, this is ugly. First, we capture the raw text and store it as a string called "rmd".
From there, the string is passed through two Liquid Filters --- liquify and markdownify.
"liquify" interperets the liquid tags in the file, and markidownify renders any markdown in the text.
Note that the filters must be applied in this order.
