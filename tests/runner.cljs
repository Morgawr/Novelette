 (ns novelette.tests.runner
   (:require [doo.runner :refer-macros [doo-tests]]
             [novelette.tests.utils]
             [novelette.tests.GUI]))

(doo-tests
  'novelette.tests.utils
  'novelette.tests.GUI)
