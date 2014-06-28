var solrApp = angular.module('solrApp', []);

solrApp.controller('SolrCtrl', function($scope, $http) {
    $scope.handlerUrl = "http://localhost:8983/solr/select";

    $scope.queries = [
    {name: 'Commits with children', definition: 'q=type:commit&fl=*,[child parentFilter=type:commit]'},
    {name: 'Commits only', definition: 'q=*:*&fq=type:commit'}
    ];

    $scope.runQuery = function(query){
        $scope.activeQuery = $scope.handlerUrl + "?" + query;
        $scope.status = null;

        console.log('http');
        $http.get($scope.activeQuery + '&wt=json')
        .success(function(data){
            $scope.status = 'Succcess';
            $scope.docs = data.response.docs;
            $scope.docsStr = JSON.stringify($scope.docs, null, '  ');
        })
        .error(function(status, data) {
            $scope.status = 'Error';
            $scope.docs = $scope.docsStr = null;
        });
    }
})