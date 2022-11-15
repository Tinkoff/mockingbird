# scrollbar-width.coffee
#
# calculate the width of the scrollbar in the browser.
# exports a function (AMD/CommonJS), or sets function
# window.scrollbarWidth (standard browser environment)
# Will cache the value, untill the function is called
# with the recalculate parameter set to true.

'use strict'

scrollbarWidth = null

getScrollbarWidth = (recalculate = false) ->
  return scrollbarWidth if scrollbarWidth? and not recalculate

  return null if document.readyState is 'loading'

  div1 = document.createElement 'div'
  div2 = document.createElement 'div'

  div1.style.width = div2.style.width = div1.style.height = div2.style.height = '100px'
  div1.style.overflow = 'scroll'
  div2.style.overflow = 'hidden'

  document.body.appendChild div1
  document.body.appendChild div2

  scrollbarWidth = Math.abs div1.scrollHeight - div2.scrollHeight

  document.body.removeChild div1
  document.body.removeChild div2

  scrollbarWidth

if typeof define is 'function' and define.amd
  define [], -> getScrollbarWidth
else if typeof exports isnt 'undefined'
  module.exports = getScrollbarWidth
else
  @getScrollbarWidth = getScrollbarWidth
