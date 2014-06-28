var solrApp = angular.module('solrApp', []);

solrApp.controller('SolrCtrl', function($scope) {
    $scope.handlerUrl = "http://localhost:8983/solr/select";

    $scope.queries = [
    {name: 'Get first 10', definition: 'q=*:*'},
    {name: 'Some other query', definition: 'q=*:*&fq=foobar'}
    ];

    $scope.runQuery = function(query){
        $scope.activeQuery = $scope.handlerUrl + "?" + query;
    }
})